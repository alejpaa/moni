# Architecture

```mermaid
flowchart LR
  Web[web: Next.js 16] -->|"@supabase/ssr"| Auth[Supabase Auth]
  Web -->|"@supabase/ssr"| API[PostgREST]
  And[android: Kotlin + Compose] -->|supabase-kt| Auth
  And -->|supabase-kt| API
  Auth --> DB[(Postgres + RLS)]
  API --> DB
```

Web and Android are independent clients of the same Supabase project. There is no custom backend; Postgres with Row Level Security is the authorization layer. Decisions: [ADR index](adr/README.md).

## Data model

Defined in `supabase/migrations/` (source of truth).

Full reference: [database.md](database.md) (ER diagram, RLS, money and installment rules, capture pipeline). Decision: [ADR-0007](adr/0007-merged-data-model.md).

- Financial data belongs to a **workspace** (`workspaces`, `workspace_members` with roles). Each user gets a personal workspace with default categories and a cash account on signup.
- Core tables: `accounts` (incl. credit cards), `categories` / `subcategories`, `transactions`, `transfers`, `installment_plans`, `budgets`, `reminders` (one-off and recurring), `merchant_rules`, `saving_goals`, `ingest_addresses` / `inbound_emails`.
- Views: `account_balances`, `installment_plan_status`, `budget_progress`.
- RLS on every table, by workspace membership.

Mirrors: `packages/shared/src/database.types.ts` (generated TS), `packages/shared/src/index.ts` (helpers) and `android/app/src/main/java/com/moni/app/data/` (Kotlin).

## Request flow

Web:
1. Browser requests a page; `web/src/proxy.ts` refreshes the session cookie and redirects to `/login` if signed out.
2. Server components create a client (`web/src/lib/supabase/server.ts`), call `getUser()` and query PostgREST with the user's JWT.
3. Postgres applies RLS, so only rows of the user's workspaces are returned. Mutations go through server actions.

Android:
1. `LoginScreen` signs in via supabase-kt; the session is held by the client.
2. `TransactionRepository` queries PostgREST with the JWT; `MoniViewModel` exposes state to Compose UI.
3. Same RLS rules apply.

## Environments

- Local: `supabase start` (API 54321, Studio 54323); Android uses `supabase.url=http://127.0.0.1:54321` plus `adb reverse tcp:54321 tcp:54321` (works on a USB phone and on the emulator).
- Production: web on Vercel (root dir `web`), DB on Supabase Cloud (`supabase link`, `supabase db push`), Android APK built with Gradle.
