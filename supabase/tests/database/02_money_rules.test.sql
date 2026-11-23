-- Money rules: credit cards, installments, transfers, budgets (current month,
-- America/Lima), dedupe and merchant rules. Run with `supabase test db`.
begin;
create extension if not exists pgtap with schema extensions;
select plan(29);

insert into auth.users (id, email, aud, role) values
  ('cccccccc-0000-0000-0000-000000000003', 'carla@test.pe', 'authenticated', 'authenticated');

set local role authenticated;
set local request.jwt.claims to '{"sub":"cccccccc-0000-0000-0000-000000000003","role":"authenticated"}';

create temp table cur as
select date_trunc('month', now() at time zone 'America/Lima')::date as month_start;

-- ---------------------------------------------------------------- accounts
select throws_ok(
  $$insert into public.accounts (name, type) values ('Visa sin datos', 'credit_card')$$,
  '23514', null, 'a credit card needs limit, closing day and due day');
select throws_ok(
  $$insert into public.accounts (name, type, credit_limit, closing_day, due_day) values ('BCP raro', 'savings', 100, 1, 1)$$,
  '23514', null, 'card fields are rejected on non-card accounts');
select lives_ok(
  $$insert into public.accounts (name, type, institution, last4, credit_limit, closing_day, due_day)
    values ('Visa BCP', 'credit_card', 'BCP', '4321', 5000, 20, 5)$$,
  'credit card account');
select lives_ok($$insert into public.accounts (name, type) values ('Yape', 'yape')$$, 'yape account kind exists');

-- ---------------------------------------------------------------- installments
select throws_ok(
  $$insert into public.installment_plans (account_id, description, total_amount, installments, installment_amount, purchased_on, first_charge_on)
    select id, 'TV', 1200, 12, 100, '2026-01-01', '2026-02-01' from public.accounts where name = 'Efectivo'$$,
  '23514', null, 'installment plans only on credit cards');

-- 12 cuotas of 100, first charged on the 1st of the month two months ago:
-- cuotas 1-3 are due (one of them this month).
select lives_ok(
  $$insert into public.installment_plans (account_id, category_id, description, merchant, total_amount, installments,
      installment_amount, purchased_on, first_charge_on)
    select a.id, c.id, 'Televisor', 'Hiraoka', 1200, 12, 100, cur.month_start - 70, (cur.month_start - interval '2 months')::date
    from public.accounts a, public.categories c, cur
    where a.name = 'Visa BCP' and c.name = 'Compras'$$,
  'installment plan on the card');
select is((select count(*)::int from public.transactions where installment_plan_id is not null), 3,
  'due installments are posted on insert');
select is((select bool_and(external_ref = 'installment:' || installment_plan_id || ':' || installment_number)
           from public.transactions where installment_plan_id is not null), true,
  'installments carry an idempotent external_ref');
select is((select note from public.transactions where installment_number = 3), 'Televisor (cuota 3/12)', 'installment note');

reset role;
select is(internal.post_due_installments(), 0, 'posting again is idempotent');
set local role authenticated;
set local request.jwt.claims to '{"sub":"cccccccc-0000-0000-0000-000000000003","role":"authenticated"}';

select results_eq(
  $$select installments_posted, installments_remaining, amount_remaining, next_charge_on
    from public.installment_plan_status$$,
  $$select 3, 9, 900.00::numeric(12,2), (cur.month_start + interval '1 month')::date from cur$$,
  'installment_plan_status');

-- ---------------------------------------------------------------- flows
insert into public.transactions (type, amount, account_id, category_id)
select 'income', 1000, a.id, c.id from public.accounts a, public.categories c
where a.name = 'Efectivo' and c.name = 'Sueldo';

insert into public.transactions (type, amount, account_id, category_id, occurred_at)
select 'expense', 200, a.id, c.id, cur.month_start + time '15:00' from public.accounts a, public.categories c, cur
where a.name = 'Visa BCP' and c.name = 'Comida';

-- pending (por revisar) and last-month spending do not count.
insert into public.transactions (type, amount, account_id, category_id, status, source)
select 'expense', 999, a.id, c.id, 'pending', 'notification' from public.accounts a, public.categories c
where a.name = 'Visa BCP' and c.name = 'Comida';
insert into public.transactions (type, amount, category_id, occurred_at)
select 'expense', 77, c.id, (cur.month_start - 1) + time '12:00' from public.categories c, cur where c.name = 'Comida';

-- Card payment: Efectivo -> Visa (a transfer, not an expense).
select lives_ok(
  $$insert into public.transfers (from_account_id, to_account_id, amount, note)
    select f.id, t.id, 150, 'Pago tarjeta' from public.accounts f, public.accounts t
    where f.name = 'Efectivo' and t.name = 'Visa BCP'$$,
  'card payment as a transfer');
select throws_ok(
  $$insert into public.transfers (from_account_id, to_account_id, amount)
    select id, id, 10 from public.accounts where name = 'Efectivo'$$,
  '23514', null, 'transfer needs two different accounts');

-- debt = 300 (cuotas) + 200 - 150 = 350; available = 5000 - 350 - 900 pending = 3750
select results_eq(
  $$select card_debt, balance, pending_installments, available_credit from public.account_balances where name = 'Visa BCP'$$,
  $$values (350.00::numeric(12,2), -350.00::numeric(12,2), 900.00::numeric(12,2), 3750.00::numeric(12,2))$$,
  'card debt and available credit (remaining installments block the line)');
select is((select balance from public.account_balances where name = 'Efectivo'), 850.00::numeric(12,2),
  'cash balance = 1000 income - 150 transfer out');

-- ---------------------------------------------------------------- budgets
insert into public.budgets (scope, limit_amount) values ('global', 2000);
insert into public.budgets (scope, category_id, limit_amount)
select 'category', id, 500 from public.categories where name = 'Comida';
insert into public.budgets (scope, category_id, limit_amount)
select 'category', id, 50 from public.categories where name = 'Compras';

select results_eq(
  $$select spent, remaining, is_standing from public.budget_progress where scope = 'global'$$,
  $$values (300.00::numeric(12,2), 1700.00::numeric(12,2), true)$$,
  'global budget: only this month''s cuota (100) + 200, not the 1200 purchase, transfers, pending or last month');
select is((select spent from public.budget_progress bp join public.categories c on c.id = bp.category_id where c.name = 'Compras'),
  100.00::numeric(12,2), 'category budget counts one cuota per month');
select is((select percent_used from public.budget_progress bp join public.categories c on c.id = bp.category_id where c.name = 'Compras'),
  200.0, 'percent_used can exceed 100');

-- A budget dated this month overrides the standing one.
insert into public.budgets (scope, category_id, limit_amount, month)
select 'category', c.id, 250, cur.month_start from public.categories c, cur where c.name = 'Comida';
select results_eq(
  $$select bp.limit_amount, bp.is_standing from public.budget_progress bp join public.categories c on c.id = bp.category_id where c.name = 'Comida'$$,
  $$values (250.00::numeric(12,2), false)$$,
  'monthly override wins over the standing budget');
select is((select count(*)::int from public.budget_progress), 3, 'one progress row per scope/category');
select throws_ok(
  $$insert into public.budgets (scope, limit_amount, month) values ('global', 1, '2026-01-15')$$,
  '23514', null, 'budget month must be the first day');
select throws_ok(
  $$insert into public.budgets (scope, limit_amount) values ('global', 5)$$,
  '23505', null, 'one standing budget per scope');

-- ---------------------------------------------------------------- dedupe
select lives_ok(
  $$insert into public.transactions (type, amount, source, status, external_ref, merchant)
    values ('expense', 12.5, 'notification', 'pending', 'yape:abc123', 'Juan Perez')$$,
  'captured transaction');
select throws_ok(
  $$insert into public.transactions (type, amount, source, status, external_ref)
    values ('expense', 12.5, 'notification', 'pending', 'yape:abc123')$$,
  '23505', null, 'same source + external_ref is rejected (PostgREST 409)');

-- ---------------------------------------------------------------- merchant rules
insert into public.merchant_rules (pattern, category_id, account_id)
select 'tambo', c.id, a.id from public.categories c, public.accounts a where c.name = 'Comida' and a.name = 'Yape';
insert into public.transactions (type, amount, merchant, source, status)
values ('expense', 8.9, 'TAMBO+ MIRAFLORES', 'notification', 'pending');
select results_eq(
  $$select c.name, a.name from public.transactions t
    join public.categories c on c.id = t.category_id join public.accounts a on a.id = t.account_id
    where t.merchant = 'TAMBO+ MIRAFLORES'$$,
  $$values ('Comida'::text, 'Yape'::text)$$,
  'merchant rule fills category and account');
insert into public.transactions (type, amount, merchant, category_id)
select 'expense', 5, 'Tambo', id from public.categories where name = 'Ocio';
select is((select c.name from public.transactions t join public.categories c on c.id = t.category_id where t.merchant = 'Tambo'),
  'Ocio', 'an explicit category is not overridden');

-- ---------------------------------------------------------------- goals
select lives_ok(
  $$insert into public.saving_goals (name, target_amount, period, account_id)
    select 'Viaje', 3000, 'yearly', id from public.accounts where name = 'Yape'$$,
  'saving goal linked to an account');
select lives_ok(
  $$insert into public.goal_contributions (goal_id, amount) select id, 120 from public.saving_goals$$,
  'goal contribution');
select is((select current_amount from public.saving_goals), 120.00::numeric(12,2), 'contributions update current_amount');

reset role;
select * from finish();
rollback;
