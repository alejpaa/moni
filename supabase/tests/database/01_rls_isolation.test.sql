-- Ownership and RLS: signup defaults, workspace isolation, roles, anon,
-- internal functions fail safely. Run with `supabase test db`.
begin;
create extension if not exists pgtap with schema extensions;
select plan(30);

insert into auth.users (id, email, aud, role, raw_user_meta_data) values
  ('aaaaaaaa-0000-0000-0000-000000000001', 'ana@test.pe', 'authenticated', 'authenticated', '{"full_name":"Ana"}'),
  ('bbbbbbbb-0000-0000-0000-000000000002', 'beto@test.pe', 'authenticated', 'authenticated', '{}');

create temp table ids as
select
  (select id from public.workspaces where created_by = 'aaaaaaaa-0000-0000-0000-000000000001') as ws_a,
  (select id from public.workspaces where created_by = 'bbbbbbbb-0000-0000-0000-000000000002') as ws_b,
  (select c.id from public.categories c join public.workspaces w on w.id = c.workspace_id
   where w.created_by = 'aaaaaaaa-0000-0000-0000-000000000001' and c.name = 'Comida') as cat_a;
grant select on ids to authenticated, anon;

-- ---------------------------------------------------------------- signup
select is((select count(*)::int from public.profiles where id = 'aaaaaaaa-0000-0000-0000-000000000001'), 1, 'signup creates a profile');
select is((select full_name from public.profiles where id = 'aaaaaaaa-0000-0000-0000-000000000001'), 'Ana', 'profile takes full_name from metadata');
select is((select role::text from public.workspace_members m, ids where m.workspace_id = ids.ws_a), 'owner', 'signup creates an owned workspace');
select is((select count(*)::int from public.categories c, ids where c.workspace_id = ids.ws_a), 17, '17 default categories');
select is((select count(*)::int from public.categories c, ids where c.workspace_id = ids.ws_a and c.kind = 'income'), 5, '5 of them are income');
select is((select icon from public.categories c, ids where c.workspace_id = ids.ws_a and c.name = 'Suscripciones'), 'repeat', 'categories carry Lucide icon keys');
select is((select type::text from public.accounts a, ids where a.workspace_id = ids.ws_a), 'cash', 'a cash account is seeded');
select is((select count(*)::int from public.ingest_addresses i, ids where i.workspace_id = ids.ws_a), 1, 'an ingest address is seeded');

-- ---------------------------------------------------------------- user A
set local role authenticated;
set local request.jwt.claims to '{"sub":"aaaaaaaa-0000-0000-0000-000000000001","role":"authenticated"}';

select lives_ok(
  $$insert into public.transactions (type, amount, category_id, note)
    select 'expense', 25, id, 'almuerzo' from public.categories where name = 'Comida'$$,
  'workspace_id and user_id default to the caller');
select is((select count(*)::int from public.transactions), 1, 'A sees own transaction');
select is((select count(*)::int from public.categories), 17, 'A sees only own categories');
select is((select count(*)::int from public.workspaces), 1, 'A sees only own workspace');
select is((select count(*)::int from public.ingest_addresses), 1, 'A sees own ingest address');

-- ---------------------------------------------------------------- user B
set local request.jwt.claims to '{"sub":"bbbbbbbb-0000-0000-0000-000000000002","role":"authenticated"}';

select is((select count(*)::int from public.transactions), 0, 'B cannot read A''s transactions');
select is((select count(*)::int from public.account_balances), 1, 'B sees only own account balances');
select is((select count(*)::int from public.ingest_addresses), 1, 'B cannot read A''s ingest token');
select throws_ok(
  $$insert into public.transactions (workspace_id, type, amount) select ws_a, 'expense', 10 from ids$$,
  '42501', null, 'B cannot insert into A''s workspace');
select throws_ok(
  $$insert into public.transactions (type, amount, category_id) select 'expense', 10, cat_a from ids$$,
  '23503', null, 'B cannot reference a category outside B''s workspace');
select is_empty(
  $$update public.transactions set amount = 1 returning id$$,
  'B cannot update A''s rows');
select throws_ok(
  $$insert into public.inbound_emails (workspace_id, user_id, message_id, from_address)
    select ws_b, 'bbbbbbbb-0000-0000-0000-000000000002', 'm1', 'x@bcp.com.pe' from ids$$,
  '42501', null, 'users cannot write inbound_emails (service role only)');
select throws_ok(
  $$update public.ingest_addresses set token = 'aaaaaaaaaaaaaaaaaaaaaaaa'$$,
  '42501', null, 'users cannot set their token directly');
select isnt(
  (select public.rotate_ingest_token()),
  (select token from public.ingest_addresses),
  'rotate_ingest_token returns a fresh token');

-- functions: internal schema fails safely; RLS helpers work.
select throws_ok(
  $$select internal.post_due_installments()$$,
  '42501', 'permission denied for schema internal', 'internal functions are not callable by API roles');
select lives_ok($$select private.is_workspace_member(ws_b) from ids$$, 'RLS helpers are callable');
select is((select 1), 1, 'backend still alive after denied call');

-- ---------------------------------------------------------------- viewer role
reset role;
insert into public.workspace_members (workspace_id, user_id, role)
select ws_a, 'bbbbbbbb-0000-0000-0000-000000000002', 'viewer' from ids;
set local role authenticated;
set local request.jwt.claims to '{"sub":"bbbbbbbb-0000-0000-0000-000000000002","role":"authenticated"}';

select is((select count(*)::int from public.transactions t, ids where t.workspace_id = ids.ws_a), 1, 'viewer can read the shared workspace');
select throws_ok(
  $$insert into public.transactions (workspace_id, type, amount) select ws_a, 'expense', 10 from ids$$,
  '42501', null, 'viewer cannot write');

-- ---------------------------------------------------------------- anon
reset role;
set local role anon;
set local request.jwt.claims to '{"role":"anon"}';
select is((select count(*)::int from public.transactions), 0, 'anon sees no transactions');
select is((select count(*)::int from public.categories), 0, 'anon sees no categories');
select throws_ok($$select * from public.account_balances$$, '42501', null, 'anon cannot read views');

reset role;
select * from finish();
rollback;
