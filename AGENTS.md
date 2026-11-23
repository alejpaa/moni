# AGENTS.md

Guide for AI agents and humans working in this repo.

## Overview

moni is a personal finance app (transactions, saving goals). A Next.js web app and a native Android app share one Supabase backend (Postgres + Auth + RLS). Architecture: [docs/architecture.md](docs/architecture.md). Decisions: [docs/adr/](docs/adr/README.md).

## Repo map

| Path | What |
|---|---|
| `web/` | Next.js 16 / React 19 / Tailwind 4 app (Vercel, root dir `web`). Read [web/AGENTS.md](web/AGENTS.md) first: Next 16 differs from training data (e.g. `middleware` is now `proxy`). |
| `packages/shared/` | TS types and helpers (`toFinanceTransaction`, `summarize`), consumed as TS source via `transpilePackages`. |
| `android/` | Native Kotlin + Jetpack Compose + supabase-kt. Gradle project, NOT in the Bun workspace. Code in `android/app/src/main/java/com/moni/app/{data,ui}`. |
| `supabase/` | Schema (`migrations/`) and local config. The schema is the contract between clients. |
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

cd android && ./gradlew :app:assembleDebug   # debug APK
```

Android emulator: use an AVD from Android Studio; it reaches the host at `http://10.0.2.2:54321`.

Config (never commit real values):
- Web: `web/.env.local` (template `web/.env.example`): `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`.
- Android: `android/local.properties` (template `local.properties.example`): `supabase.url`, `supabase.anonKey`.

## Conventions

- Schema changes are a new file in `supabase/migrations/`. Never edit an applied migration.
- Enable RLS on every table; policies use `(select auth.uid()) = user_id`.
- Keep types in sync with the schema: `packages/shared/src/index.ts` (web) and `android/.../data/` (Kotlin models).
- UI copy is in Spanish.
- Commit straight to `main`.
- Use Bun, not npm/pnpm/yarn. Do not reintroduce `.agents/`, `.gemini/` or `skills-lock.json` (removed on purpose).

## Deploy

Web: Vercel (root dir `web`). DB: Supabase Cloud (`supabase link`, `supabase db push`). Android: `./gradlew assembleDebug` for now; GitHub Actions / Play internal track later.
