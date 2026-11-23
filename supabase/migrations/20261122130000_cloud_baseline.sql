-- Baseline of the schema that was created on Supabase Cloud (project moni-finanzas)
-- outside of this repo (dashboard / another tool) before 2026-11-22.
--
-- It reproduces the cloud state exactly on top of the two earlier migrations so
-- that `supabase db reset` matches production. It was marked as applied on the
-- remote with `supabase migration repair --status applied 20261122130000`, so it
-- never runs there. Do not "fix" anything here: fixes go in later migrations
-- (see 20261123120000_merge_planning_and_capture.sql and docs/database.md).

create schema if not exists private;
grant usage on schema private to authenticated;

-- ---------------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------------
create type public.account_type as enum ('cash', 'bank', 'credit_card', 'debit_card', 'wallet', 'investment', 'other');
create type public.alert_level as enum ('warning_80', 'exceeded_100');
create type public.budget_scope as enum ('global', 'category', 'subcategory');
create type public.reminder_kind as enum ('payable', 'receivable');
create type public.workspace_role as enum ('owner', 'admin', 'member', 'viewer');

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------
create table public.workspaces (
  id uuid default gen_random_uuid() not null,
  name text not null,
  currency text default 'USD'::text not null,
  created_by uuid not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.workspace_members (
  workspace_id uuid not null,
  user_id uuid not null,
  role public.workspace_role default 'member'::public.workspace_role not null,
  created_at timestamptz default now() not null
);

create table public.profiles (
  id uuid not null,
  email text,
  full_name text,
  currency text default 'USD'::text not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.accounts (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  name text not null,
  type public.account_type default 'bank'::public.account_type not null,
  opening_balance numeric(12,2) default 0 not null,
  currency text default 'USD'::text not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.categories (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  name text not null,
  color text default '#14b8a6'::text not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.subcategories (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  category_id uuid not null,
  name text not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.budgets (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  scope public.budget_scope not null,
  category_id uuid,
  subcategory_id uuid,
  month date not null,
  limit_amount numeric(12,2) not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null,
  constraint budgets_check check (
    ((scope = 'global'::public.budget_scope) and (category_id is null) and (subcategory_id is null))
    or ((scope = 'category'::public.budget_scope) and (category_id is not null) and (subcategory_id is null))
    or ((scope = 'subcategory'::public.budget_scope) and (category_id is not null) and (subcategory_id is not null))
  ),
  constraint budgets_limit_amount_check check (limit_amount > (0)::numeric)
);

create table public.budget_alerts (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  budget_id uuid not null,
  level public.alert_level not null,
  triggered_at timestamptz default now() not null
);

create table public.goal_contributions (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  goal_id uuid not null,
  account_id uuid,
  user_id uuid not null,
  amount numeric(12,2) not null,
  contributed_at timestamptz default now() not null,
  note text,
  created_at timestamptz default now() not null,
  constraint goal_contributions_amount_check check (amount > (0)::numeric)
);

create table public.imports (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  uploaded_by uuid not null,
  kind text default 'ofx'::text not null,
  file_name text not null,
  storage_path text,
  status text default 'uploaded'::text not null,
  created_at timestamptz default now() not null
);

create table public.monthly_reports (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  month date not null,
  storage_path text,
  generated_at timestamptz default now() not null
);

create table public.reminders (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  user_id uuid not null,
  kind public.reminder_kind not null,
  title text not null,
  amount numeric(12,2),
  due_date date not null,
  is_done boolean default false not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create table public.transaction_attachments (
  id uuid default gen_random_uuid() not null,
  workspace_id uuid not null,
  transaction_id uuid,
  uploaded_by uuid not null,
  storage_path text not null,
  file_name text not null,
  content_type text,
  size_bytes bigint,
  created_at timestamptz default now() not null
);

alter table public.saving_goals
  add column workspace_id uuid,
  add column created_by uuid,
  add column currency text default 'USD'::text not null;

alter table public.transactions
  add column workspace_id uuid,
  add column account_id uuid,
  add column category_id uuid,
  add column subcategory_id uuid,
  add column currency text default 'USD'::text not null,
  add column source text default 'web_manual'::text not null,
  add column ai_confidence numeric(4,3),
  add column raw_input text;

-- ---------------------------------------------------------------------------
-- Keys and indexes
-- ---------------------------------------------------------------------------
alter table only public.accounts add constraint accounts_pkey primary key (id);
alter table only public.budget_alerts add constraint budget_alerts_budget_id_level_triggered_at_key unique (budget_id, level, triggered_at);
alter table only public.budget_alerts add constraint budget_alerts_pkey primary key (id);
alter table only public.budgets add constraint budgets_pkey primary key (id);
alter table only public.categories add constraint categories_pkey primary key (id);
alter table only public.categories add constraint categories_workspace_id_name_key unique (workspace_id, name);
alter table only public.goal_contributions add constraint goal_contributions_pkey primary key (id);
alter table only public.imports add constraint imports_pkey primary key (id);
alter table only public.monthly_reports add constraint monthly_reports_pkey primary key (id);
alter table only public.monthly_reports add constraint monthly_reports_workspace_id_month_key unique (workspace_id, month);
alter table only public.profiles add constraint profiles_pkey primary key (id);
alter table only public.reminders add constraint reminders_pkey primary key (id);
alter table only public.subcategories add constraint subcategories_category_id_name_key unique (category_id, name);
alter table only public.subcategories add constraint subcategories_pkey primary key (id);
alter table only public.transaction_attachments add constraint transaction_attachments_pkey primary key (id);
alter table only public.workspace_members add constraint workspace_members_pkey primary key (workspace_id, user_id);
alter table only public.workspaces add constraint workspaces_pkey primary key (id);

create index accounts_workspace_id_idx on public.accounts using btree (workspace_id);
create index budgets_workspace_id_month_idx on public.budgets using btree (workspace_id, month desc);
create index categories_workspace_id_idx on public.categories using btree (workspace_id);
create index reminders_workspace_id_due_date_idx on public.reminders using btree (workspace_id, due_date);
create index subcategories_workspace_id_idx on public.subcategories using btree (workspace_id);
create index transactions_workspace_id_occurred_at_idx on public.transactions using btree (workspace_id, occurred_at desc);
create index workspace_members_user_id_idx on public.workspace_members using btree (user_id);

alter table only public.accounts add constraint accounts_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.budget_alerts add constraint budget_alerts_budget_id_fkey foreign key (budget_id) references public.budgets(id) on delete cascade;
alter table only public.budget_alerts add constraint budget_alerts_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.budgets add constraint budgets_category_id_fkey foreign key (category_id) references public.categories(id) on delete cascade;
alter table only public.budgets add constraint budgets_subcategory_id_fkey foreign key (subcategory_id) references public.subcategories(id) on delete cascade;
alter table only public.budgets add constraint budgets_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.categories add constraint categories_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.goal_contributions add constraint goal_contributions_account_id_fkey foreign key (account_id) references public.accounts(id) on delete set null;
alter table only public.goal_contributions add constraint goal_contributions_goal_id_fkey foreign key (goal_id) references public.saving_goals(id) on delete cascade;
alter table only public.goal_contributions add constraint goal_contributions_user_id_fkey foreign key (user_id) references auth.users(id) on delete cascade;
alter table only public.goal_contributions add constraint goal_contributions_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.imports add constraint imports_uploaded_by_fkey foreign key (uploaded_by) references auth.users(id) on delete cascade;
alter table only public.imports add constraint imports_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.monthly_reports add constraint monthly_reports_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.profiles add constraint profiles_id_fkey foreign key (id) references auth.users(id) on delete cascade;
alter table only public.reminders add constraint reminders_user_id_fkey foreign key (user_id) references auth.users(id) on delete cascade;
alter table only public.reminders add constraint reminders_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.saving_goals add constraint saving_goals_created_by_fkey foreign key (created_by) references auth.users(id) on delete set null;
alter table only public.saving_goals add constraint saving_goals_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.subcategories add constraint subcategories_category_id_fkey foreign key (category_id) references public.categories(id) on delete cascade;
alter table only public.subcategories add constraint subcategories_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.transaction_attachments add constraint transaction_attachments_transaction_id_fkey foreign key (transaction_id) references public.transactions(id) on delete cascade;
alter table only public.transaction_attachments add constraint transaction_attachments_uploaded_by_fkey foreign key (uploaded_by) references auth.users(id) on delete cascade;
alter table only public.transaction_attachments add constraint transaction_attachments_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.transactions add constraint transactions_account_id_fkey foreign key (account_id) references public.accounts(id) on delete set null;
alter table only public.transactions add constraint transactions_category_id_fkey foreign key (category_id) references public.categories(id) on delete set null;
alter table only public.transactions add constraint transactions_subcategory_id_fkey foreign key (subcategory_id) references public.subcategories(id) on delete set null;
alter table only public.transactions add constraint transactions_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.workspace_members add constraint workspace_members_user_id_fkey foreign key (user_id) references auth.users(id) on delete cascade;
alter table only public.workspace_members add constraint workspace_members_workspace_id_fkey foreign key (workspace_id) references public.workspaces(id) on delete cascade;
alter table only public.workspaces add constraint workspaces_created_by_fkey foreign key (created_by) references auth.users(id) on delete cascade;

-- ---------------------------------------------------------------------------
-- Functions
-- ---------------------------------------------------------------------------
create or replace function private.is_workspace_admin(target_workspace_id uuid) returns boolean
language sql stable security definer
set search_path to ''
as $$
  select exists (
    select 1
    from public.workspace_members wm
    where wm.workspace_id = target_workspace_id
      and wm.user_id = auth.uid()
      and wm.role in ('owner', 'admin')
  );
$$;

create or replace function private.is_workspace_member(target_workspace_id uuid) returns boolean
language sql stable security definer
set search_path to ''
as $$
  select exists (
    select 1
    from public.workspace_members wm
    where wm.workspace_id = target_workspace_id
      and wm.user_id = auth.uid()
  );
$$;

grant all on function private.is_workspace_admin(uuid) to authenticated;
grant all on function private.is_workspace_member(uuid) to authenticated;

create or replace function private.handle_new_user() returns trigger
language plpgsql security definer
set search_path to ''
as $$
declare
  new_workspace_id uuid;
begin
  insert into public.profiles (id, email, full_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)))
  on conflict (id) do nothing;

  insert into public.workspaces (name, created_by)
  values ('Mi espacio financiero', new.id)
  returning id into new_workspace_id;

  insert into public.workspace_members (workspace_id, user_id, role)
  values (new_workspace_id, new.id, 'owner');

  insert into public.categories (workspace_id, name, color)
  values
    (new_workspace_id, 'Alimentacion', '#f97316'),
    (new_workspace_id, 'Vivienda', '#0ea5e9'),
    (new_workspace_id, 'Transporte', '#8b5cf6'),
    (new_workspace_id, 'Ingresos', '#22c55e')
  on conflict do nothing;

  return new;
end;
$$;

create or replace function private.update_goal_current_amount() returns trigger
language plpgsql security definer
set search_path to ''
as $$
declare
  target_goal_id uuid;
begin
  if tg_op = 'DELETE' then
    target_goal_id := old.goal_id;
  else
    target_goal_id := new.goal_id;
  end if;

  update public.saving_goals sg
  set current_amount = coalesce((
    select sum(gc.amount)
    from public.goal_contributions gc
    where gc.goal_id = target_goal_id
  ), 0),
  updated_at = now()
  where sg.id = target_goal_id;

  if tg_op = 'DELETE' then
    return old;
  end if;

  return new;
end;
$$;

create or replace function public.ensure_personal_workspace()
returns table(id uuid, name text, currency text)
language plpgsql security definer
set search_path to ''
as $$
declare
  current_user_id uuid;
  workspace_id uuid;
begin
  current_user_id := auth.uid();

  if current_user_id is null then
    raise exception 'Not authenticated';
  end if;

  select wm.workspace_id into workspace_id
  from public.workspace_members wm
  where wm.user_id = current_user_id
  order by wm.created_at asc
  limit 1;

  if workspace_id is null then
    insert into public.profiles (id, email, full_name)
    select u.id, u.email, coalesce(split_part(u.email, '@', 1), 'Usuario')
    from auth.users u
    where u.id = current_user_id
    on conflict on constraint profiles_pkey do nothing;

    insert into public.workspaces (name, created_by)
    values ('Mi espacio financiero', current_user_id)
    returning workspaces.id into workspace_id;

    insert into public.workspace_members (workspace_id, user_id, role)
    values (workspace_id, current_user_id, 'owner')
    on conflict do nothing;

    insert into public.categories (workspace_id, name, color)
    values
      (workspace_id, 'Alimentacion', '#f97316'),
      (workspace_id, 'Vivienda', '#0ea5e9'),
      (workspace_id, 'Transporte', '#8b5cf6'),
      (workspace_id, 'Ingresos', '#22c55e')
    on conflict do nothing;
  end if;

  return query
  select w.id, w.name, w.currency
  from public.workspaces w
  where w.id = workspace_id;
end;
$$;

-- ---------------------------------------------------------------------------
-- Triggers
-- ---------------------------------------------------------------------------
create or replace trigger accounts_set_updated_at before update on public.accounts for each row execute function public.set_updated_at();
create or replace trigger budgets_set_updated_at before update on public.budgets for each row execute function public.set_updated_at();
create or replace trigger categories_set_updated_at before update on public.categories for each row execute function public.set_updated_at();
create or replace trigger goal_contributions_update_goal_amount after insert or delete or update on public.goal_contributions for each row execute function private.update_goal_current_amount();
create or replace trigger reminders_set_updated_at before update on public.reminders for each row execute function public.set_updated_at();
create or replace trigger subcategories_set_updated_at before update on public.subcategories for each row execute function public.set_updated_at();

create or replace trigger on_auth_user_created_create_finance_workspace
after insert on auth.users
for each row execute function private.handle_new_user();

-- ---------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------
alter table public.accounts enable row level security;
alter table public.budget_alerts enable row level security;
alter table public.budgets enable row level security;
alter table public.categories enable row level security;
alter table public.goal_contributions enable row level security;
alter table public.imports enable row level security;
alter table public.monthly_reports enable row level security;
alter table public.profiles enable row level security;
alter table public.reminders enable row level security;
alter table public.subcategories enable row level security;
alter table public.transaction_attachments enable row level security;
alter table public.workspace_members enable row level security;
alter table public.workspaces enable row level security;

-- Member policies (identical shape on every workspace-owned table).
do $$
declare
  t text;
begin
  foreach t in array array[
    'accounts', 'budget_alerts', 'budgets', 'categories', 'goal_contributions', 'imports',
    'monthly_reports', 'reminders', 'subcategories', 'transaction_attachments'
  ] loop
    execute format('create policy %I on public.%I for delete to authenticated using (private.is_workspace_member(workspace_id))', t || '_member_delete', t);
    execute format('create policy %I on public.%I for insert to authenticated with check (private.is_workspace_member(workspace_id))', t || '_member_insert', t);
    execute format('create policy %I on public.%I for select to authenticated using (private.is_workspace_member(workspace_id))', t || '_member_select', t);
    execute format('create policy %I on public.%I for update to authenticated using (private.is_workspace_member(workspace_id)) with check (private.is_workspace_member(workspace_id))', t || '_member_update', t);
  end loop;
end;
$$;

create policy profiles_insert_own on public.profiles for insert to authenticated with check (id = auth.uid());
create policy profiles_select_own on public.profiles for select to authenticated using (id = auth.uid());
create policy profiles_update_own on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

create policy saving_goals_workspace_delete on public.saving_goals for delete to authenticated using (private.is_workspace_member(workspace_id));
create policy saving_goals_workspace_insert on public.saving_goals for insert to authenticated with check (private.is_workspace_member(workspace_id) and (user_id = auth.uid()));
create policy saving_goals_workspace_select on public.saving_goals for select to authenticated using (private.is_workspace_member(workspace_id));
create policy saving_goals_workspace_update on public.saving_goals for update to authenticated using (private.is_workspace_member(workspace_id)) with check (private.is_workspace_member(workspace_id));

create policy transactions_workspace_delete on public.transactions for delete to authenticated using (private.is_workspace_member(workspace_id));
create policy transactions_workspace_insert on public.transactions for insert to authenticated with check (private.is_workspace_member(workspace_id) and (user_id = auth.uid()));
create policy transactions_workspace_select on public.transactions for select to authenticated using (private.is_workspace_member(workspace_id));
create policy transactions_workspace_update on public.transactions for update to authenticated using (private.is_workspace_member(workspace_id)) with check (private.is_workspace_member(workspace_id));

create policy workspace_members_admin_update on public.workspace_members for update to authenticated using (private.is_workspace_admin(workspace_id)) with check (private.is_workspace_admin(workspace_id));
create policy workspace_members_creator_insert on public.workspace_members for insert to authenticated with check (
  (user_id = auth.uid()) and (exists (
    select 1 from public.workspaces w
    where (w.id = workspace_members.workspace_id) and (w.created_by = auth.uid())
  ))
);
create policy workspace_members_member_select on public.workspace_members for select to authenticated using (private.is_workspace_member(workspace_id));

create policy workspaces_admin_update on public.workspaces for update to authenticated using (private.is_workspace_admin(id)) with check (private.is_workspace_admin(id));
create policy workspaces_member_select on public.workspaces for select to authenticated using (private.is_workspace_member(id));
create policy workspaces_owner_insert on public.workspaces for insert to authenticated with check (created_by = auth.uid());

-- ---------------------------------------------------------------------------
-- Grants (as on the cloud; Supabase default privileges grant ALL to API roles,
-- RLS is what restricts access)
-- ---------------------------------------------------------------------------
grant all on table
  public.accounts, public.budget_alerts, public.budgets, public.categories, public.goal_contributions,
  public.imports, public.monthly_reports, public.profiles, public.reminders, public.saving_goals,
  public.subcategories, public.transaction_attachments, public.transactions, public.workspace_members,
  public.workspaces
to anon, authenticated, service_role;
