-- Merge the cloud workspace model with Moni's planning and capture design.
-- Docs: docs/database.md, ADR-0007 (merged data model), ADR-0006 (capture).
--
-- Ownership: every financial row belongs to a workspace (workspace_id NOT NULL).
-- RLS allows a row when its workspace is one of the caller's memberships
-- (private.my_workspace_ids(), evaluated once per statement). Writes need a
-- non-viewer role. Child rows reference parents through composite
-- (id, workspace_id) foreign keys, so a row can never point at another
-- workspace's account, category, plan, etc., even with a guessed UUID.
-- `user_id` / `uploaded_by` columns are the author (default auth.uid()), not the
-- owner. Money is PEN only (numeric(12,2), currency = 'PEN').
--
-- Function placement (see docs/database.md#gotcha):
--   private  : RLS helpers and column defaults. USAGE granted to authenticated
--              (policies run as the caller); not exposed by PostgREST.
--   internal : triggers, seeding, jobs. No USAGE for API roles; a direct call
--              fails with "permission denied for schema internal".
-- Never `revoke execute` on a function: on Supabase PG 17.6 calling a function
-- whose EXECUTE was revoked segfaults the backend and puts the DB in recovery.

create schema if not exists internal;
revoke all on schema internal from public, anon, authenticated, service_role;
grant usage on schema private to service_role;

-- ---------------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------------
create type public.capture_source as enum ('manual', 'email', 'notification', 'statement', 'installment', 'ai');
create type public.review_status as enum ('confirmed', 'pending', 'ignored');
create type public.recurring_frequency as enum ('weekly', 'monthly', 'yearly');
create type public.email_parse_status as enum ('received', 'parsed', 'unrecognized', 'duplicate', 'error');

-- ---------------------------------------------------------------------------
-- RLS helpers (private)
-- ---------------------------------------------------------------------------
create or replace function private.my_workspace_ids()
returns setof uuid
language sql stable security definer
set search_path = ''
as $$
  select wm.workspace_id from public.workspace_members wm
  where wm.user_id = (select auth.uid());
$$;

create or replace function private.my_writable_workspace_ids()
returns setof uuid
language sql stable security definer
set search_path = ''
as $$
  select wm.workspace_id from public.workspace_members wm
  where wm.user_id = (select auth.uid()) and wm.role <> 'viewer';
$$;

-- The caller's personal (oldest) workspace. Column default for workspace_id,
-- so single-workspace clients may omit it. Null for service_role / anon.
create or replace function private.current_workspace_id()
returns uuid
language sql stable security definer
set search_path = ''
as $$
  select wm.workspace_id from public.workspace_members wm
  where wm.user_id = (select auth.uid())
  order by wm.created_at, wm.workspace_id
  limit 1;
$$;

grant execute on function private.my_workspace_ids(), private.my_writable_workspace_ids(),
  private.current_workspace_id() to authenticated, service_role;

-- ---------------------------------------------------------------------------
-- Workspaces, members, profiles: PEN, updated_at
-- ---------------------------------------------------------------------------
update public.workspaces set currency = 'PEN' where currency <> 'PEN';
alter table public.workspaces
  alter column currency set default 'PEN',
  add constraint workspaces_currency_pen check (currency = 'PEN'),
  add constraint workspaces_name_not_blank check (length(trim(name)) > 0);

update public.profiles set currency = 'PEN' where currency <> 'PEN';
alter table public.profiles
  alter column currency set default 'PEN',
  add constraint profiles_currency_pen check (currency = 'PEN');

create index workspaces_created_by_idx on public.workspaces (created_by);

create trigger workspaces_set_updated_at before update on public.workspaces for each row execute function public.set_updated_at();
create trigger profiles_set_updated_at before update on public.profiles for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Accounts (+ credit card details)
-- ---------------------------------------------------------------------------
update public.accounts set currency = 'PEN' where currency <> 'PEN';
alter table public.accounts
  alter column workspace_id set default private.current_workspace_id(),
  alter column currency set default 'PEN',
  add column institution text,
  add column last4 text,
  add column color text,
  add column archived boolean not null default false,
  add column credit_limit numeric(12,2),
  add column closing_day smallint,
  add column due_day smallint,
  add constraint accounts_currency_pen check (currency = 'PEN'),
  add constraint accounts_name_not_blank check (length(trim(name)) > 0),
  add constraint accounts_last4_format check (last4 ~ '^[0-9]{4}$'),
  add constraint accounts_credit_limit_positive check (credit_limit > 0),
  add constraint accounts_closing_day_range check (closing_day between 1 and 31),
  add constraint accounts_due_day_range check (due_day between 1 and 31),
  -- Card fields are required on credit cards and forbidden elsewhere.
  add constraint accounts_card_fields check (
    (type = 'credit_card' and credit_limit is not null and closing_day is not null and due_day is not null)
    or (type <> 'credit_card' and credit_limit is null and closing_day is null and due_day is null)
  ),
  add constraint accounts_id_workspace_key unique (id, workspace_id);
comment on column public.accounts.opening_balance is
  'Balance when the account was added. For credit cards: the debt at that moment (positive = owed).';
create unique index accounts_workspace_name_key on public.accounts (workspace_id, lower(name));

-- ---------------------------------------------------------------------------
-- Categories (+ kind, Lucide icon) and subcategories
-- ---------------------------------------------------------------------------
alter table public.categories
  alter column workspace_id set default private.current_workspace_id(),
  alter column color set default '#6F5FC0',
  add column kind public.transaction_type not null default 'expense',
  add column icon text,
  add column archived boolean not null default false,
  add constraint categories_name_not_blank check (length(trim(name)) > 0),
  add constraint categories_icon_format check (icon ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  add constraint categories_id_workspace_key unique (id, workspace_id);
comment on column public.categories.icon is 'Lucide icon key (docs/design.md#iconos), e.g. utensils.';

alter table public.subcategories
  alter column workspace_id set default private.current_workspace_id(),
  add constraint subcategories_name_not_blank check (length(trim(name)) > 0),
  add constraint subcategories_id_workspace_key unique (id, workspace_id),
  add constraint subcategories_id_category_key unique (id, category_id),
  drop constraint subcategories_category_id_fkey,
  add constraint subcategories_category_fkey foreign key (category_id, workspace_id)
    references public.categories (id, workspace_id) on delete cascade;
create index subcategories_category_idx on public.subcategories (category_id, workspace_id);

-- ---------------------------------------------------------------------------
-- Merchant rules: pattern -> category / account
-- ---------------------------------------------------------------------------
create table public.merchant_rules (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null default private.current_workspace_id()
    references public.workspaces (id) on delete cascade,
  -- Case-insensitive substring matched against transactions.merchant.
  pattern text not null check (length(trim(pattern)) > 0),
  category_id uuid,
  subcategory_id uuid,
  account_id uuid,
  priority smallint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  foreign key (category_id, workspace_id) references public.categories (id, workspace_id) on delete cascade,
  foreign key (subcategory_id, category_id) references public.subcategories (id, category_id) on delete set null (subcategory_id),
  foreign key (account_id, workspace_id) references public.accounts (id, workspace_id) on delete set null (account_id),
  check (category_id is not null or account_id is not null),
  check (subcategory_id is null or category_id is not null)
);
create unique index merchant_rules_workspace_pattern_key on public.merchant_rules (workspace_id, lower(pattern));
create index merchant_rules_category_idx on public.merchant_rules (category_id, workspace_id);
create index merchant_rules_subcategory_idx on public.merchant_rules (subcategory_id, category_id);
create index merchant_rules_account_idx on public.merchant_rules (account_id, workspace_id);

-- ---------------------------------------------------------------------------
-- Reminders become recurring items (subscriptions, fixed bills) too.
-- frequency null = one-off reminder (cloud behaviour); otherwise the app moves
-- due_date forward each time it is paid. is_done = finished / cancelled.
-- kind: payable = expense, receivable = income.
-- ---------------------------------------------------------------------------
alter table public.reminders
  alter column workspace_id set default private.current_workspace_id(),
  alter column user_id set default auth.uid(),
  add column frequency public.recurring_frequency,
  add column remind_days_before smallint not null default 2,
  add column account_id uuid,
  add column category_id uuid,
  add column note text,
  add constraint reminders_title_not_blank check (length(trim(title)) > 0),
  add constraint reminders_amount_positive check (amount > 0),
  add constraint reminders_remind_days_range check (remind_days_before between 0 and 30),
  add constraint reminders_id_workspace_key unique (id, workspace_id),
  add constraint reminders_account_fkey foreign key (account_id, workspace_id)
    references public.accounts (id, workspace_id) on delete set null (account_id),
  add constraint reminders_category_fkey foreign key (category_id, workspace_id)
    references public.categories (id, workspace_id) on delete set null (category_id);
create index reminders_account_idx on public.reminders (account_id, workspace_id);
create index reminders_category_idx on public.reminders (category_id, workspace_id);
create index reminders_user_idx on public.reminders (user_id);

-- ---------------------------------------------------------------------------
-- Installment plans (compras en cuotas) on a credit card
-- ---------------------------------------------------------------------------
create table public.installment_plans (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null default private.current_workspace_id()
    references public.workspaces (id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  account_id uuid not null,
  description text not null check (length(trim(description)) > 0),
  merchant text,
  category_id uuid,
  subcategory_id uuid,
  -- Purchase price. The monthly charge (installment_amount) may include interest.
  total_amount numeric(12,2) not null check (total_amount > 0),
  installments smallint not null check (installments between 2 and 72),
  installment_amount numeric(12,2) not null check (installment_amount > 0),
  -- Annual effective rate in percent (TEA), informational. Null = sin intereses.
  interest_rate numeric(6,3) check (interest_rate >= 0),
  purchased_on date not null default ((now() at time zone 'America/Lima')::date),
  first_charge_on date not null,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, workspace_id),
  foreign key (account_id, workspace_id) references public.accounts (id, workspace_id) on delete cascade,
  foreign key (category_id, workspace_id) references public.categories (id, workspace_id) on delete set null (category_id),
  foreign key (subcategory_id, category_id) references public.subcategories (id, category_id) on delete set null (subcategory_id),
  check (first_charge_on >= purchased_on),
  check (subcategory_id is null or category_id is not null)
);
create index installment_plans_workspace_idx on public.installment_plans (workspace_id);
create index installment_plans_user_idx on public.installment_plans (user_id);
create index installment_plans_account_idx on public.installment_plans (account_id, workspace_id);
create index installment_plans_category_idx on public.installment_plans (category_id, workspace_id);
create index installment_plans_subcategory_idx on public.installment_plans (subcategory_id, category_id);

-- ---------------------------------------------------------------------------
-- Budgets: month null = standing monthly budget; a dated row (first day of a
-- month) overrides it for that month.
-- ---------------------------------------------------------------------------
alter table public.budgets
  alter column workspace_id set default private.current_workspace_id(),
  alter column month drop not null,
  add constraint budgets_month_first_day check (month = date_trunc('month', month)::date),
  add constraint budgets_id_workspace_key unique (id, workspace_id),
  drop constraint budgets_category_id_fkey,
  drop constraint budgets_subcategory_id_fkey,
  add constraint budgets_category_fkey foreign key (category_id, workspace_id)
    references public.categories (id, workspace_id) on delete cascade,
  add constraint budgets_subcategory_fkey foreign key (subcategory_id, category_id)
    references public.subcategories (id, category_id) on delete cascade;
create unique index budgets_scope_month_key
  on public.budgets (workspace_id, scope, category_id, subcategory_id, month) nulls not distinct;
create index budgets_category_idx on public.budgets (category_id, workspace_id);
create index budgets_subcategory_idx on public.budgets (subcategory_id, category_id);

alter table public.budget_alerts
  drop constraint budget_alerts_budget_id_fkey,
  add constraint budget_alerts_budget_fkey foreign key (budget_id, workspace_id)
    references public.budgets (id, workspace_id) on delete cascade;
create index budget_alerts_workspace_idx on public.budget_alerts (workspace_id);
create index budget_alerts_budget_idx on public.budget_alerts (budget_id, workspace_id);

-- ---------------------------------------------------------------------------
-- Saving goals (optionally linked to an account) and contributions
-- ---------------------------------------------------------------------------
update public.saving_goals set currency = 'PEN' where currency <> 'PEN';
alter table public.saving_goals
  alter column workspace_id set default private.current_workspace_id(),
  alter column user_id set default auth.uid(),
  alter column currency set default 'PEN',
  add column account_id uuid,
  add constraint saving_goals_currency_pen check (currency = 'PEN'),
  add constraint saving_goals_name_not_blank check (length(trim(name)) > 0),
  add constraint saving_goals_id_workspace_key unique (id, workspace_id),
  add constraint saving_goals_account_fkey foreign key (account_id, workspace_id)
    references public.accounts (id, workspace_id) on delete set null (account_id);
create index saving_goals_workspace_idx on public.saving_goals (workspace_id);
create index saving_goals_account_idx on public.saving_goals (account_id, workspace_id);

alter table public.goal_contributions
  alter column workspace_id set default private.current_workspace_id(),
  alter column user_id set default auth.uid(),
  drop constraint goal_contributions_goal_id_fkey,
  drop constraint goal_contributions_account_id_fkey,
  add constraint goal_contributions_goal_fkey foreign key (goal_id, workspace_id)
    references public.saving_goals (id, workspace_id) on delete cascade,
  add constraint goal_contributions_account_fkey foreign key (account_id, workspace_id)
    references public.accounts (id, workspace_id) on delete set null (account_id);
create index goal_contributions_goal_idx on public.goal_contributions (goal_id, workspace_id);
create index goal_contributions_account_idx on public.goal_contributions (account_id, workspace_id);
create index goal_contributions_user_idx on public.goal_contributions (user_id);
create index goal_contributions_workspace_idx on public.goal_contributions (workspace_id);

-- ---------------------------------------------------------------------------
-- Transactions: workspace-owned, linked, capture metadata
-- ---------------------------------------------------------------------------
alter table public.transactions
  alter column workspace_id set default private.current_workspace_id(),
  alter column user_id set default auth.uid(),
  alter column currency set default 'PEN',
  add column merchant text,
  add column status public.review_status not null default 'confirmed',
  add column external_ref text,
  add column installment_plan_id uuid,
  add column installment_number smallint,
  add column reminder_id uuid,
  add constraint transactions_ai_confidence_range check (ai_confidence between 0 and 1),
  add constraint transactions_installment_number_positive check (installment_number > 0),
  add constraint transactions_installment_pair check ((installment_plan_id is null) = (installment_number is null)),
  add constraint transactions_id_workspace_key unique (id, workspace_id),
  drop constraint transactions_account_id_fkey,
  drop constraint transactions_category_id_fkey,
  drop constraint transactions_subcategory_id_fkey,
  add constraint transactions_account_fkey foreign key (account_id, workspace_id)
    references public.accounts (id, workspace_id) on delete set null (account_id),
  add constraint transactions_category_fkey foreign key (category_id, workspace_id)
    references public.categories (id, workspace_id) on delete set null (category_id),
  -- Two checks on the subcategory: same workspace, and child of the category.
  add constraint transactions_subcategory_fkey foreign key (subcategory_id, workspace_id)
    references public.subcategories (id, workspace_id) on delete set null (subcategory_id),
  add constraint transactions_subcategory_category_fkey foreign key (subcategory_id, category_id)
    references public.subcategories (id, category_id) on delete set null (subcategory_id),
  add constraint transactions_installment_plan_fkey foreign key (installment_plan_id, workspace_id)
    references public.installment_plans (id, workspace_id) on delete cascade,
  add constraint transactions_reminder_fkey foreign key (reminder_id, workspace_id)
    references public.reminders (id, workspace_id) on delete set null (reminder_id);

-- source: free text ('web_manual') -> capture_source enum.
alter table public.transactions alter column source drop default;
alter table public.transactions alter column source type public.capture_source using (
  case
    when source in ('manual', 'email', 'notification', 'statement', 'installment', 'ai') then source
    when source ilike '%email%' or source ilike '%mail%' then 'email'
    when source ilike '%notif%' then 'notification'
    when source ilike '%import%' or source ilike '%ofx%' or source ilike '%csv%' or source ilike '%statement%' then 'statement'
    when source ilike '%ai%' or source ilike '%voice%' then 'ai'
    else 'manual'
  end
)::public.capture_source;
alter table public.transactions alter column source set default 'manual';

create unique index transactions_dedupe_key on public.transactions (workspace_id, source, external_ref)
  where external_ref is not null;
create index transactions_pending_idx on public.transactions (workspace_id, occurred_at desc) where status = 'pending';
create index transactions_account_idx on public.transactions (account_id, workspace_id);
create index transactions_category_idx on public.transactions (category_id, workspace_id);
create index transactions_subcategory_idx on public.transactions (subcategory_id, category_id);
create index transactions_subcategory_workspace_idx on public.transactions (subcategory_id, workspace_id);
create index transactions_installment_plan_idx on public.transactions (installment_plan_id, workspace_id);
create index transactions_reminder_idx on public.transactions (reminder_id, workspace_id);

alter table public.transaction_attachments
  alter column workspace_id set default private.current_workspace_id(),
  alter column uploaded_by set default auth.uid(),
  drop constraint transaction_attachments_transaction_id_fkey,
  add constraint transaction_attachments_transaction_fkey foreign key (transaction_id, workspace_id)
    references public.transactions (id, workspace_id) on delete cascade;
create index transaction_attachments_transaction_idx on public.transaction_attachments (transaction_id, workspace_id);
create index transaction_attachments_workspace_idx on public.transaction_attachments (workspace_id);
create index transaction_attachments_uploaded_by_idx on public.transaction_attachments (uploaded_by);

alter table public.imports
  alter column workspace_id set default private.current_workspace_id(),
  alter column uploaded_by set default auth.uid();
create index imports_workspace_idx on public.imports (workspace_id);
create index imports_uploaded_by_idx on public.imports (uploaded_by);

alter table public.monthly_reports
  alter column workspace_id set default private.current_workspace_id();

-- ---------------------------------------------------------------------------
-- Transfers between own accounts (card payments, BCP -> Yape, withdrawals).
-- Not income or expense.
-- ---------------------------------------------------------------------------
create table public.transfers (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null default private.current_workspace_id()
    references public.workspaces (id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  from_account_id uuid not null,
  to_account_id uuid not null,
  amount numeric(12,2) not null check (amount > 0),
  occurred_at timestamptz not null default now(),
  note text,
  source public.capture_source not null default 'manual',
  status public.review_status not null default 'confirmed',
  external_ref text,
  raw_input text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  foreign key (from_account_id, workspace_id) references public.accounts (id, workspace_id) on delete cascade,
  foreign key (to_account_id, workspace_id) references public.accounts (id, workspace_id) on delete cascade,
  check (from_account_id <> to_account_id)
);
create unique index transfers_dedupe_key on public.transfers (workspace_id, source, external_ref)
  where external_ref is not null;
create index transfers_workspace_occurred_idx on public.transfers (workspace_id, occurred_at desc);
create index transfers_from_idx on public.transfers (from_account_id, workspace_id);
create index transfers_to_idx on public.transfers (to_account_id, workspace_id);
create index transfers_user_idx on public.transfers (user_id);

-- ---------------------------------------------------------------------------
-- Email capture: one secret forwarding address per member and workspace
-- (<token>@alertas.<domain>) and a log written only by the ingest Edge
-- Function (service role).
-- ---------------------------------------------------------------------------
create table public.ingest_addresses (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  token text not null unique default replace(gen_random_uuid()::text, '-', '')
    check (token ~ '^[a-z0-9]{16,64}$'),
  created_at timestamptz not null default now(),
  rotated_at timestamptz,
  unique (workspace_id, user_id)
);
create index ingest_addresses_user_idx on public.ingest_addresses (user_id);

create table public.inbound_emails (
  id uuid primary key default gen_random_uuid(),
  workspace_id uuid not null references public.workspaces (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  message_id text not null,
  from_address text not null,
  subject text,
  body_text text,
  received_at timestamptz not null default now(),
  parse_status public.email_parse_status not null default 'received',
  parser text,
  transaction_id uuid,
  error text,
  created_at timestamptz not null default now(),
  unique (user_id, message_id),
  foreign key (transaction_id, workspace_id) references public.transactions (id, workspace_id) on delete set null (transaction_id)
);
create index inbound_emails_user_received_idx on public.inbound_emails (user_id, received_at desc);
create index inbound_emails_workspace_idx on public.inbound_emails (workspace_id);
create index inbound_emails_transaction_idx on public.inbound_emails (transaction_id, workspace_id);

-- ---------------------------------------------------------------------------
-- updated_at triggers for new tables
-- ---------------------------------------------------------------------------
create trigger merchant_rules_set_updated_at before update on public.merchant_rules for each row execute function public.set_updated_at();
create trigger installment_plans_set_updated_at before update on public.installment_plans for each row execute function public.set_updated_at();
create trigger transfers_set_updated_at before update on public.transfers for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- Internal functions and triggers
-- ---------------------------------------------------------------------------

-- Default categories (Lucide icons) and a cash account for a workspace.
create or replace function internal.seed_workspace_defaults(p_workspace_id uuid)
returns void
language plpgsql security definer
set search_path = ''
as $$
begin
  insert into public.categories (workspace_id, name, kind, icon)
  values
    (p_workspace_id, 'Comida', 'expense', 'utensils'),
    (p_workspace_id, 'Supermercado', 'expense', 'shopping-cart'),
    (p_workspace_id, 'Transporte', 'expense', 'bus'),
    (p_workspace_id, 'Casa', 'expense', 'house'),
    (p_workspace_id, 'Servicios', 'expense', 'zap'),
    (p_workspace_id, 'Salud', 'expense', 'heart-pulse'),
    (p_workspace_id, 'Ocio', 'expense', 'clapperboard'),
    (p_workspace_id, 'Compras', 'expense', 'shopping-bag'),
    (p_workspace_id, 'Educación', 'expense', 'graduation-cap'),
    (p_workspace_id, 'Viajes', 'expense', 'plane'),
    (p_workspace_id, 'Suscripciones', 'expense', 'repeat'),
    (p_workspace_id, 'Otros gastos', 'expense', 'arrow-up-right'),
    (p_workspace_id, 'Sueldo', 'income', 'briefcase'),
    (p_workspace_id, 'Regalos', 'income', 'gift'),
    (p_workspace_id, 'Ventas', 'income', 'store'),
    (p_workspace_id, 'Reembolsos', 'income', 'rotate-ccw'),
    (p_workspace_id, 'Otros ingresos', 'income', 'arrow-down-left')
  on conflict do nothing;

  insert into public.accounts (workspace_id, name, type)
  values (p_workspace_id, 'Efectivo', 'cash')
  on conflict do nothing;
end;
$$;

-- New workspace: creator becomes owner, defaults are seeded.
create or replace function internal.on_workspace_created()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
begin
  insert into public.workspace_members (workspace_id, user_id, role)
  values (new.id, new.created_by, 'owner')
  on conflict do nothing;
  perform internal.seed_workspace_defaults(new.id);
  return new;
end;
$$;

create trigger workspaces_after_insert
after insert on public.workspaces
for each row execute function internal.on_workspace_created();

-- New member: gets a forwarding address for that workspace.
create or replace function internal.on_member_added()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
begin
  insert into public.ingest_addresses (workspace_id, user_id)
  values (new.workspace_id, new.user_id)
  on conflict do nothing;
  return new;
end;
$$;

create trigger workspace_members_after_insert
after insert on public.workspace_members
for each row execute function internal.on_member_added();

-- Signup: profile + personal workspace (the workspace trigger does the rest).
create or replace function internal.handle_new_user()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, email, full_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)))
  on conflict (id) do nothing;

  insert into public.workspaces (name, created_by)
  values ('Mi espacio financiero', new.id);

  return new;
end;
$$;

drop trigger on_auth_user_created_create_finance_workspace on auth.users;
create trigger on_auth_user_created_create_finance_workspace
after insert on auth.users
for each row execute function internal.handle_new_user();
drop function private.handle_new_user();

-- RPC kept from the cloud: returns (creating if needed) the caller's personal workspace.
create or replace function public.ensure_personal_workspace()
returns table (id uuid, name text, currency text)
language plpgsql security definer
set search_path = ''
as $$
declare
  current_user_id uuid := auth.uid();
  ws_id uuid;
begin
  if current_user_id is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  ws_id := private.current_workspace_id();

  if ws_id is null then
    insert into public.profiles (id, email, full_name)
    select u.id, u.email, coalesce(split_part(u.email, '@', 1), 'Usuario')
    from auth.users u
    where u.id = current_user_id
    on conflict on constraint profiles_pkey do nothing;

    insert into public.workspaces (name, created_by)
    values ('Mi espacio financiero', current_user_id)
    returning workspaces.id into ws_id;
  end if;

  return query
  select w.id, w.name, w.currency
  from public.workspaces w
  where w.id = ws_id;
end;
$$;

-- RPC: new random forwarding token for the caller in a workspace (default:
-- personal). The old address stops working immediately.
create or replace function public.rotate_ingest_token(p_workspace_id uuid default null)
returns text
language plpgsql security definer
set search_path = ''
as $$
declare
  ws_id uuid := coalesce(p_workspace_id, private.current_workspace_id());
  new_token text;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if ws_id is null or not private.is_workspace_member(ws_id) then
    raise exception 'Not a member of this workspace' using errcode = '42501';
  end if;

  insert into public.ingest_addresses (workspace_id, user_id)
  values (ws_id, auth.uid())
  on conflict (workspace_id, user_id) do update
    set token = replace(gen_random_uuid()::text, '-', ''), rotated_at = now()
  returning token into new_token;

  return new_token;
end;
$$;

-- Installment plans must be on a credit card of the same workspace.
create or replace function internal.ensure_installment_card()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
begin
  if not exists (
    select 1 from public.accounts a
    where a.id = new.account_id and a.workspace_id = new.workspace_id and a.type = 'credit_card'
  ) then
    raise exception 'installment_plans.account_id must be a credit_card account of the same workspace'
      using errcode = '23514';
  end if;
  return new;
end;
$$;

create trigger installment_plans_card_check
before insert or update of account_id, workspace_id on public.installment_plans
for each row execute function internal.ensure_installment_card();

-- Post each monthly installment as a transaction once it is due (Lima date).
-- Idempotent: external_ref 'installment:<plan>:<n>' + transactions_dedupe_key.
-- Only these monthly charges count as spending (ADR-0007).
create or replace function internal.post_due_installments(p_plan_id uuid default null)
returns integer
language plpgsql security definer
set search_path = ''
as $$
declare
  today date := (now() at time zone 'America/Lima')::date;
  inserted integer;
begin
  insert into public.transactions (
    workspace_id, user_id, account_id, category_id, subcategory_id, type, amount, occurred_at,
    merchant, note, source, status, external_ref, installment_plan_id, installment_number
  )
  select
    p.workspace_id,
    p.user_id,
    p.account_id,
    p.category_id,
    p.subcategory_id,
    'expense',
    p.installment_amount,
    ((p.first_charge_on + make_interval(months => n - 1))::date + time '12:00') at time zone 'America/Lima',
    p.merchant,
    p.description || ' (cuota ' || n || '/' || p.installments || ')',
    'installment',
    'confirmed',
    'installment:' || p.id || ':' || n,
    p.id,
    n
  from public.installment_plans p
  cross join lateral generate_series(1, p.installments) as n
  where (p_plan_id is null or p.id = p_plan_id)
    and (p.first_charge_on + make_interval(months => n - 1))::date <= today
  on conflict (workspace_id, source, external_ref) where external_ref is not null do nothing;

  get diagnostics inserted = row_count;
  return inserted;
end;
$$;

create or replace function internal.on_installment_plan_changed()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
begin
  perform internal.post_due_installments(new.id);
  return new;
end;
$$;

create trigger installment_plans_post_due
after insert or update of first_charge_on, installments, installment_amount on public.installment_plans
for each row execute function internal.on_installment_plan_changed();

-- Auto-categorize by merchant: on insert, fill category/account from the
-- highest-priority matching rule of the same workspace.
create or replace function internal.apply_merchant_rules()
returns trigger
language plpgsql security definer
set search_path = ''
as $$
declare
  r record;
begin
  if new.merchant is null or (new.category_id is not null and new.account_id is not null) then
    return new;
  end if;

  select mr.category_id, mr.subcategory_id, mr.account_id into r
  from public.merchant_rules mr
  left join public.categories c on c.id = mr.category_id
  where mr.workspace_id = new.workspace_id
    and position(lower(mr.pattern) in lower(new.merchant)) > 0
    and (mr.category_id is null or c.kind = new.type)
  order by mr.priority desc, length(mr.pattern) desc
  limit 1;

  if found then
    if new.category_id is null and r.category_id is not null then
      new.category_id := r.category_id;
      new.subcategory_id := coalesce(new.subcategory_id, r.subcategory_id);
    end if;
    new.account_id := coalesce(new.account_id, r.account_id);
  end if;
  return new;
end;
$$;

create trigger transactions_apply_merchant_rules
before insert on public.transactions
for each row execute function internal.apply_merchant_rules();

-- ---------------------------------------------------------------------------
-- Backfill existing data
-- ---------------------------------------------------------------------------
do $$
declare
  u record;
  m record;
begin
  -- 1. Old seeded categories of the cloud trigger -> the new default set (ids kept).
  update public.categories c set name = 'Comida', icon = 'utensils'
  where c.name = 'Alimentacion'
    and not exists (select 1 from public.categories x where x.workspace_id = c.workspace_id and x.name = 'Comida');
  update public.categories c set name = 'Casa', icon = 'house'
  where c.name = 'Vivienda'
    and not exists (select 1 from public.categories x where x.workspace_id = c.workspace_id and x.name = 'Casa');
  update public.categories c set name = 'Otros ingresos', kind = 'income', icon = 'arrow-down-left'
  where c.name = 'Ingresos'
    and not exists (select 1 from public.categories x where x.workspace_id = c.workspace_id and x.name = 'Otros ingresos');
  update public.categories set kind = 'income' where name = 'Ingresos';
  update public.categories set icon = 'bus' where name = 'Transporte' and icon is null;

  -- 2. Existing workspaces: defaults and forwarding addresses.
  for m in select id from public.workspaces loop
    perform internal.seed_workspace_defaults(m.id);
  end loop;
  insert into public.ingest_addresses (workspace_id, user_id)
  select wm.workspace_id, wm.user_id from public.workspace_members wm
  on conflict do nothing;

  -- 3. Users created before the signup trigger: profile + personal workspace.
  for u in
    select au.id, au.email, au.raw_user_meta_data from auth.users au
    where not exists (select 1 from public.workspace_members wm where wm.user_id = au.id)
  loop
    insert into public.profiles (id, email, full_name)
    values (u.id, u.email, coalesce(u.raw_user_meta_data ->> 'full_name', split_part(u.email, '@', 1)))
    on conflict (id) do nothing;
    insert into public.workspaces (name, created_by) values ('Mi espacio financiero', u.id);
  end loop;

  -- 4. Rows without a workspace -> the author's personal workspace.
  update public.transactions t set workspace_id = (
    select wm.workspace_id from public.workspace_members wm
    where wm.user_id = t.user_id order by wm.created_at, wm.workspace_id limit 1)
  where t.workspace_id is null;
  update public.saving_goals g set workspace_id = (
    select wm.workspace_id from public.workspace_members wm
    where wm.user_id = g.user_id order by wm.created_at, wm.workspace_id limit 1)
  where g.workspace_id is null;
  -- saving_goals.created_by duplicated user_id (author); fill it before dropping.
  update public.saving_goals set user_id = created_by where created_by is not null and user_id <> created_by;

  -- 5. Free-text transactions.category -> categories (created when missing).
  insert into public.categories (workspace_id, name, kind)
  select distinct on (t.workspace_id, trim(t.category)) t.workspace_id, trim(t.category), t.type
  from public.transactions t
  where t.category_id is null
    and not exists (
      select 1 from public.categories c
      where c.workspace_id = t.workspace_id and lower(c.name) = lower(trim(t.category)))
  order by t.workspace_id, trim(t.category), t.created_at
  on conflict do nothing;

  update public.transactions t set category_id = c.id
  from public.categories c
  where t.category_id is null
    and c.workspace_id = t.workspace_id
    and lower(c.name) = lower(trim(t.category));

  -- 6. Legacy transactions without an account -> the workspace's cash account.
  update public.transactions t set account_id = a.id
  from public.accounts a
  where t.account_id is null
    and a.workspace_id = t.workspace_id
    and a.type = 'cash'
    and lower(a.name) = 'efectivo';

  update public.transactions set currency = 'PEN' where currency <> 'PEN';
end;
$$;

alter table public.transactions
  alter column workspace_id set not null,
  add constraint transactions_currency_pen check (currency = 'PEN'),
  drop column category;  -- replaced by category_id (drops transactions_category_not_blank)

alter table public.saving_goals
  alter column workspace_id set not null,
  drop column created_by;  -- redundant with user_id; 0 rows on the cloud

-- ---------------------------------------------------------------------------
-- pg_cron: post due installments daily at 00:05 America/Lima (05:05 UTC)
-- ---------------------------------------------------------------------------
create extension if not exists pg_cron with schema pg_catalog;
select cron.schedule('post-due-installments', '5 5 * * *', $$select internal.post_due_installments()$$);

-- ---------------------------------------------------------------------------
-- Views (security_invoker: the caller's RLS applies)
-- ---------------------------------------------------------------------------
create view public.account_balances
with (security_invoker = true)
as
with tx as (
  select account_id,
    sum(case when type = 'income' then amount else 0 end) as income,
    sum(case when type = 'expense' then amount else 0 end) as expense
  from public.transactions
  where status = 'confirmed' and account_id is not null
  group by account_id
),
tin as (
  select to_account_id as account_id, sum(amount) as amount
  from public.transfers where status = 'confirmed' group by to_account_id
),
tout as (
  select from_account_id as account_id, sum(amount) as amount
  from public.transfers where status = 'confirmed' group by from_account_id
),
posted as (
  select installment_plan_id, count(*)::int as n
  from public.transactions where installment_plan_id is not null
  group by installment_plan_id
),
pending_installments as (
  -- Installments not yet charged: they still block the card's credit line.
  select p.account_id, sum(p.installment_amount * greatest(p.installments - coalesce(posted.n, 0), 0)) as amount
  from public.installment_plans p
  left join posted on posted.installment_plan_id = p.id
  group by p.account_id
),
flows as (
  select a.*,
    coalesce(tx.income, 0) as income,
    coalesce(tx.expense, 0) as expense,
    coalesce(tin.amount, 0) as transfers_in,
    coalesce(tout.amount, 0) as transfers_out,
    coalesce(pi.amount, 0) as pending_installments
  from public.accounts a
  left join tx on tx.account_id = a.id
  left join tin on tin.account_id = a.id
  left join tout on tout.account_id = a.id
  left join pending_installments pi on pi.account_id = a.id
)
select
  f.id as account_id,
  f.workspace_id,
  f.name,
  f.type,
  f.institution,
  f.last4,
  f.color,
  f.archived,
  f.currency,
  -- Money you have (negative for a card with debt).
  case when f.type = 'credit_card'
    then -(f.opening_balance + f.expense - f.income - f.transfers_in + f.transfers_out)
    else f.opening_balance + f.income - f.expense + f.transfers_in - f.transfers_out
  end::numeric(12,2) as balance,
  case when f.type = 'credit_card'
    then (f.opening_balance + f.expense - f.income - f.transfers_in + f.transfers_out)::numeric(12,2)
  end as card_debt,
  f.pending_installments::numeric(12,2) as pending_installments,
  f.credit_limit,
  case when f.type = 'credit_card'
    then (f.credit_limit - (f.opening_balance + f.expense - f.income - f.transfers_in + f.transfers_out)
          - f.pending_installments)::numeric(12,2)
  end as available_credit,
  f.closing_day,
  f.due_day
from flows f;

create view public.installment_plan_status
with (security_invoker = true)
as
select
  p.id as installment_plan_id,
  p.workspace_id,
  p.account_id,
  p.description,
  p.merchant,
  p.category_id,
  p.total_amount,
  p.installments,
  p.installment_amount,
  p.first_charge_on,
  coalesce(posted.n, 0) as installments_posted,
  greatest(p.installments - coalesce(posted.n, 0), 0) as installments_remaining,
  (p.installment_amount * greatest(p.installments - coalesce(posted.n, 0), 0))::numeric(12,2) as amount_remaining,
  case when coalesce(posted.n, 0) < p.installments
    then (p.first_charge_on + make_interval(months => coalesce(posted.n, 0)))::date
  end as next_charge_on
from public.installment_plans p
left join (
  select installment_plan_id, count(*)::int as n
  from public.transactions where installment_plan_id is not null
  group by installment_plan_id
) posted on posted.installment_plan_id = p.id;

-- Current month (America/Lima). Per scope/category/subcategory, a budget dated
-- this month overrides the standing (month null) one.
create view public.budget_progress
with (security_invoker = true)
as
with cur as (
  select
    date_trunc('month', now() at time zone 'America/Lima')::date as month,
    date_trunc('month', now() at time zone 'America/Lima') at time zone 'America/Lima' as starts_at,
    (date_trunc('month', now() at time zone 'America/Lima') + interval '1 month') at time zone 'America/Lima' as ends_at
),
effective as (
  select distinct on (b.workspace_id, b.scope, b.category_id, b.subcategory_id) b.*
  from public.budgets b, cur
  where b.month is null or b.month = cur.month
  order by b.workspace_id, b.scope, b.category_id, b.subcategory_id, b.month nulls last
),
month_tx as (
  select t.workspace_id, t.category_id, t.subcategory_id, t.amount
  from public.transactions t, cur
  where t.type = 'expense'
    and t.status = 'confirmed'
    and t.occurred_at >= cur.starts_at
    and t.occurred_at < cur.ends_at
)
select
  e.id as budget_id,
  e.workspace_id,
  e.scope,
  e.category_id,
  e.subcategory_id,
  cur.month,
  (e.month is null) as is_standing,
  e.limit_amount,
  coalesce(sum(m.amount), 0)::numeric(12,2) as spent,
  (e.limit_amount - coalesce(sum(m.amount), 0))::numeric(12,2) as remaining,
  round(coalesce(sum(m.amount), 0) * 100 / e.limit_amount, 1) as percent_used
from effective e
cross join cur
left join month_tx m
  on m.workspace_id = e.workspace_id
 and (
   e.scope = 'global'
   or (e.scope = 'category' and m.category_id = e.category_id)
   or (e.scope = 'subcategory' and m.subcategory_id = e.subcategory_id)
 )
group by e.id, e.workspace_id, e.scope, e.category_id, e.subcategory_id, cur.month, e.month, e.limit_amount;

revoke all on public.account_balances, public.installment_plan_status, public.budget_progress from anon;
grant select on public.account_balances, public.installment_plan_status, public.budget_progress to authenticated;

-- ---------------------------------------------------------------------------
-- RLS policies (rewritten for every table: one consistent, fast shape)
-- ---------------------------------------------------------------------------
alter table public.merchant_rules enable row level security;
alter table public.installment_plans enable row level security;
alter table public.transfers enable row level security;
alter table public.ingest_addresses enable row level security;
alter table public.inbound_emails enable row level security;

-- Drop every existing policy on the tables we manage, including the legacy
-- user_id policies on transactions/saving_goals: they were OR-ed with the
-- workspace ones and let a user insert rows into any workspace_id.
do $$
declare
  p record;
begin
  for p in
    select policyname, tablename from pg_policies
    where schemaname = 'public' and tablename in (
      'accounts', 'budget_alerts', 'budgets', 'categories', 'goal_contributions', 'imports',
      'monthly_reports', 'reminders', 'saving_goals', 'subcategories', 'transaction_attachments',
      'transactions', 'workspace_members', 'workspaces', 'profiles'
    )
  loop
    execute format('drop policy %I on public.%I', p.policyname, p.tablename);
  end loop;
end;
$$;

-- Workspace-owned tables. author = column that must equal the caller on insert.
do $$
declare
  t record;
  author_check text;
begin
  for t in
    select * from (values
      ('accounts', null),
      ('budget_alerts', null),
      ('budgets', null),
      ('categories', null),
      ('subcategories', null),
      ('merchant_rules', null),
      ('monthly_reports', null),
      ('goal_contributions', 'user_id'),
      ('installment_plans', 'user_id'),
      ('reminders', 'user_id'),
      ('saving_goals', 'user_id'),
      ('transactions', 'user_id'),
      ('transfers', 'user_id'),
      ('imports', 'uploaded_by'),
      ('transaction_attachments', 'uploaded_by')
    ) as v(tbl, author)
  loop
    author_check := case when t.author is null then ''
      else format(' and %I = (select auth.uid())', t.author) end;
    execute format(
      'create policy %I on public.%I for select to authenticated
         using (workspace_id in (select private.my_workspace_ids()))',
      t.tbl || '_member_select', t.tbl);
    execute format(
      'create policy %I on public.%I for insert to authenticated
         with check (workspace_id in (select private.my_writable_workspace_ids())%s)',
      t.tbl || '_member_insert', t.tbl, author_check);
    execute format(
      'create policy %I on public.%I for update to authenticated
         using (workspace_id in (select private.my_writable_workspace_ids()))
         with check (workspace_id in (select private.my_writable_workspace_ids()))',
      t.tbl || '_member_update', t.tbl);
    execute format(
      'create policy %I on public.%I for delete to authenticated
         using (workspace_id in (select private.my_writable_workspace_ids()))',
      t.tbl || '_member_delete', t.tbl);
  end loop;
end;
$$;

create policy workspaces_member_select on public.workspaces for select to authenticated
  using (id in (select private.my_workspace_ids()));
create policy workspaces_owner_insert on public.workspaces for insert to authenticated
  with check (created_by = (select auth.uid()));
create policy workspaces_admin_update on public.workspaces for update to authenticated
  using (private.is_workspace_admin(id)) with check (private.is_workspace_admin(id));

create policy workspace_members_member_select on public.workspace_members for select to authenticated
  using (workspace_id in (select private.my_workspace_ids()));
create policy workspace_members_creator_insert on public.workspace_members for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.workspaces w
                where w.id = workspace_members.workspace_id and w.created_by = (select auth.uid()))
  );
create policy workspace_members_admin_update on public.workspace_members for update to authenticated
  using (private.is_workspace_admin(workspace_id)) with check (private.is_workspace_admin(workspace_id));

create policy profiles_select_own on public.profiles for select to authenticated
  using (id = (select auth.uid()));
create policy profiles_insert_own on public.profiles for insert to authenticated
  with check (id = (select auth.uid()));
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));

-- Forwarding address: each member sees only their own token. Created by
-- triggers, rotated with public.rotate_ingest_token().
revoke all on public.ingest_addresses from anon, authenticated;
grant select on public.ingest_addresses to authenticated;
create policy ingest_addresses_select_own on public.ingest_addresses for select to authenticated
  using (user_id = (select auth.uid()) and workspace_id in (select private.my_workspace_ids()));

-- Inbound emails: written only by the ingest Edge Function (service role);
-- the forwarding user can read and delete theirs.
revoke all on public.inbound_emails from anon, authenticated;
grant select, delete on public.inbound_emails to authenticated;
create policy inbound_emails_select_own on public.inbound_emails for select to authenticated
  using (user_id = (select auth.uid()) and workspace_id in (select private.my_workspace_ids()));
create policy inbound_emails_delete_own on public.inbound_emails for delete to authenticated
  using (user_id = (select auth.uid()) and workspace_id in (select private.my_workspace_ids()));
