-- Pin search_path so the trigger function cannot be hijacked via a
-- user-controlled schema (flagged by the Supabase security linter).
alter function public.set_updated_at() set search_path = '';

-- Wrap auth.uid() in a subselect so Postgres evaluates it once per query
-- instead of once per row.
alter policy "users_can_select_own_transactions" on public.transactions
  using ((select auth.uid()) = user_id);
alter policy "users_can_insert_own_transactions" on public.transactions
  with check ((select auth.uid()) = user_id);
alter policy "users_can_update_own_transactions" on public.transactions
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
alter policy "users_can_delete_own_transactions" on public.transactions
  using ((select auth.uid()) = user_id);

alter policy "users_can_select_own_saving_goals" on public.saving_goals
  using ((select auth.uid()) = user_id);
alter policy "users_can_insert_own_saving_goals" on public.saving_goals
  with check ((select auth.uid()) = user_id);
alter policy "users_can_update_own_saving_goals" on public.saving_goals
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
alter policy "users_can_delete_own_saving_goals" on public.saving_goals
  using ((select auth.uid()) = user_id);

-- Reject blank categories.
alter table public.transactions
  add constraint transactions_category_not_blank check (length(trim(category)) > 0);
