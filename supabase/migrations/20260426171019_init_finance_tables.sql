create extension if not exists "pgcrypto";

do $$
begin
  if not exists (select 1 from pg_type where typname = 'transaction_type') then
    create type public.transaction_type as enum ('income', 'expense');
  end if;

  if not exists (select 1 from pg_type where typname = 'goal_period') then
    create type public.goal_period as enum ('monthly', 'yearly');
  end if;
end
$$;

create table if not exists public.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type public.transaction_type not null,
  amount numeric(12,2) not null check (amount > 0),
  category text not null,
  occurred_at timestamptz not null default now(),
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.saving_goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  target_amount numeric(12,2) not null check (target_amount > 0),
  current_amount numeric(12,2) not null default 0 check (current_amount >= 0),
  period public.goal_period not null,
  deadline date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists transactions_user_id_occurred_at_idx
  on public.transactions(user_id, occurred_at desc);

create index if not exists saving_goals_user_id_idx
  on public.saving_goals(user_id);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists transactions_set_updated_at on public.transactions;
create trigger transactions_set_updated_at
before update on public.transactions
for each row
execute procedure public.set_updated_at();

drop trigger if exists saving_goals_set_updated_at on public.saving_goals;
create trigger saving_goals_set_updated_at
before update on public.saving_goals
for each row
execute procedure public.set_updated_at();

alter table public.transactions enable row level security;
alter table public.saving_goals enable row level security;

grant select, insert, update, delete on table public.transactions to authenticated;
grant select, insert, update, delete on table public.saving_goals to authenticated;

create policy "users_can_select_own_transactions"
on public.transactions
for select
to authenticated
using (auth.uid() = user_id);

create policy "users_can_insert_own_transactions"
on public.transactions
for insert
to authenticated
with check (auth.uid() = user_id);

create policy "users_can_update_own_transactions"
on public.transactions
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "users_can_delete_own_transactions"
on public.transactions
for delete
to authenticated
using (auth.uid() = user_id);

create policy "users_can_select_own_saving_goals"
on public.saving_goals
for select
to authenticated
using (auth.uid() = user_id);

create policy "users_can_insert_own_saving_goals"
on public.saving_goals
for insert
to authenticated
with check (auth.uid() = user_id);

create policy "users_can_update_own_saving_goals"
on public.saving_goals
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

create policy "users_can_delete_own_saving_goals"
on public.saving_goals
for delete
to authenticated
using (auth.uid() = user_id);
