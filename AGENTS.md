# AGENTS.md

Guide for AI agents and humans working in this repo.

## Overview

moni is a personal finance app (transactions, saving goals). A Next.js web app and a native Android app share one Supabase backend (Postgres + Auth + RLS). Architecture: [docs/architecture.md](docs/architecture.md). Data model, RLS and money rules: [docs/database.md](docs/database.md). Decisions: [docs/adr/](docs/adr/README.md). Visual design: [docs/design.md](docs/design.md) (read before touching UI). Roadmap for automatic capture: [docs/plan-automatizacion.md](docs/plan-automatizacion.md).

## Repo map

| Path | What |
|---|---|
| `web/` | Next.js 16 / React 19 / Tailwind 4 app (Vercel, root dir `web`). Read [web/AGENTS.md](web/AGENTS.md) first: Next 16 differs from training data (e.g. `middleware` is now `proxy`). |
| `packages/shared/` | TS types and helpers (`toFinanceTransaction`, `summarize`), consumed as TS source via `transpilePackages`. `src/database.types.ts` is generated from the schema. |
| `android/` | Native Kotlin + Jetpack Compose + supabase-kt. Gradle project, NOT in the Bun workspace. Code in `android/app/src/main/java/com/moni/app/{data,ui}`. |
| `supabase/` | Schema (`migrations/`), pgTAP tests (`tests/database/`) and local config. The schema is the contract between clients. |
| `docs/` | Architecture and ADRs. |

## Commands

```bash
bun install                 # install workspace deps
bun dev                     # all dev tasks via turbo (web on :3000)
bun run dev:web             # web only
bun run typecheck | lint | build

supabase start              # local stack (Docker): API 54321, Studio 54323
supabase status             # prints URL and anon key for env files
supabase db reset           # re-apply all migrations locally
supabase test db            # pgTAP tests (RLS, money rules)
supabase db lint --level warning && supabase db advisors

cd android && ./gradlew :app:assembleDebug   # debug APK
```

Android device (USB phone or emulator): run `adb reverse tcp:54321 tcp:54321` (again after every reconnect), then `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The app calls `http://127.0.0.1:54321`. JDK: `JAVA_HOME=/opt/homebrew/opt/openjdk@17`.

Config (never commit real values):
- Web: `web/.env.local` (template `web/.env.example`): `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`.
- Android: `android/local.properties` (template `local.properties.example`): `supabase.url`, `supabase.anonKey`.

## Conventions

- Schema changes are a new file in `supabase/migrations/`. Never edit an applied migration. Never change the cloud schema from the dashboard: write a migration.
- Financial data is owned by a **workspace**, not a user (ADR-0007). Every such table has `workspace_id NOT NULL` and composite `(id, workspace_id)` foreign keys. Enable RLS on every table; policies use `workspace_id in (select private.my_workspace_ids())` for reads and `private.my_writable_workspace_ids()` for writes. Per-user tables use `(select auth.uid()) = user_id`.
- Never `revoke execute` on a function: on Supabase PG 17.6 calling it crashes the database. Internal functions go in the `internal` schema (no USAGE for API roles). See [docs/database.md](docs/database.md#gotcha).
- Money is PEN only, `numeric(12,2)`. Only `status = 'confirmed'` counts. Transfers are never income or expense. For installments, only the monthly cuota counts.
- After a migration: run `supabase test db`, regenerate `packages/shared/src/database.types.ts` (`supabase gen types typescript --project-id xyswhlasctqtqxefndkr`), and update the Kotlin models in `android/.../data/`.
- UI copy is in Spanish.
- Commit straight to `main`.
- Use Bun, not npm/pnpm/yarn. Do not reintroduce `.agents/`, `.gemini/` or `skills-lock.json` (removed on purpose).

## Deploy

Web: Vercel (root dir `web`). DB: Supabase Cloud (`supabase link`, `supabase db push`). Android: `./gradlew assembleDebug` for now; GitHub Actions / Play internal track later.
