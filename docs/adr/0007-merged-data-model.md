# ADR-0007: Merged data model (cloud workspaces + planning and capture)

## Status
Accepted

## Date
2026-11-23

## Context
The Supabase Cloud project `moni-finanzas` had a schema created outside the repo: workspaces with members and roles, profiles, accounts, categories, subcategories, budgets, budget alerts, reminders, goal contributions, attachments, imports and monthly reports. It also had about 65 RLS policies. `transactions` and `saving_goals` had both the repo's per-user policies and new workspace policies. A repo migration designed per-user accounts, cards, transfers, installments and email capture failed on the cloud (`relation "accounts" already exists`).

Problems found on the cloud:
- The legacy `user_id` policies were OR-ed with the workspace policies. A user could insert a transaction into **any** `workspace_id`, or with none.
- Foreign keys were single-column, so a row could point at another workspace's account or category.
- `transactions.workspace_id` and `saving_goals.workspace_id` were nullable. Three of four users (created before the signup trigger) had no workspace. The only transaction had no workspace.
- `auth.uid()` was not wrapped in several policies. There were duplicate permissive policies and unindexed foreign keys. Defaults were `currency = 'USD'` although Moni is PEN only.

## Decision
Keep the cloud's structure and make the repo its source of truth again:

1. `20261122130000_cloud_baseline.sql` reproduces the cloud schema exactly. It is marked applied on the remote.
2. **Ownership is the workspace.** Every financial row has `workspace_id NOT NULL` (defaulting to the caller's personal workspace). RLS uses `workspace_id in (select private.my_workspace_ids())`, and writes exclude `viewer`. Composite `(id, workspace_id)` foreign keys make cross-workspace references impossible. `user_id` / `uploaded_by` remain as the author (default `auth.uid()`, must equal the caller on insert). The legacy per-user policies are dropped.
3. **Extend rather than duplicate:**
   - the `account_type` enum is mapped (`bank`→`savings`, `debit_card`→`debit`, + `yape`), and credit card fields are columns on `accounts`;
   - `reminders` gains `frequency` / `remind_days_before` and covers recurring items;
   - `budgets.month` becomes nullable (standing budget) with dated overrides;
   - `transactions.source` becomes the `capture_source` enum (`web_manual`→`manual`), and `raw_input` / `ai_confidence` are reused.
4. **New tables:** `transfers`, `installment_plans`, `merchant_rules`, `ingest_addresses`, `inbound_emails`. **New views:** `account_balances`, `installment_plan_status`, `budget_progress`.
5. **Installments:** only the monthly cuota counts as spending. Cuotas are posted idempotently (`external_ref` + unique index) by a trigger and a daily pg_cron job. Remaining cuotas block available credit.
6. **Removed:** `transactions.category` (free text; migrated to `category_id`) and `saving_goals.created_by` (duplicated `user_id`, no rows). Nothing else of the cloud schema was dropped. The four seeded categories were renamed in place (ids kept) to the new default set.
7. **Backfill:** every user got a profile and a workspace. Existing rows got a workspace, category and cash account. Currency was set to PEN.
8. Internal functions live in a schema without USAGE (`internal`). Function EXECUTE is never revoked, because that crashes this Postgres build (see `docs/database.md#gotcha`).

## Alternatives Considered
- **Drop the cloud tables and apply the per-user design**: loses the user's structure (workspaces, roles, subcategories) and goes against the request to merge.
- **Keep per-user ownership next to workspaces**: two ownership models, ambiguous RLS, and it keeps the cross-workspace insert hole.
- **Separate `credit_cards` and `recurring_items` tables**: parallel structures to what the cloud already had (`accounts.type`, `reminders`).
- **Keep `transactions.category` for compatibility**: two sources of truth for the category; the apps have to change anyway for workspaces and accounts.

## Consequences
- Clients must use `category_id` (and may send `account_id`, `workspace_id`). The deployed web and Android builds that write `category` break until they are updated.
- Sharing a workspace with another user is possible in the schema. There is no UI or invite flow yet.
- ADR-0002's RLS rule `(select auth.uid()) = user_id` is superseded by workspace membership for financial tables.
- The advisor warns that two `SECURITY DEFINER` RPCs are executable by `anon`; this is accepted because both reject anonymous callers.
- Types are generated (`supabase gen types`) into `packages/shared/src/database.types.ts`; Kotlin models are still kept by hand.
