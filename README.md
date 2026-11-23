# moni

Personal finance app: track income/expenses and saving goals. Next.js web app and native Android app on a shared Supabase backend (Postgres + Auth + RLS).

| Path | What |
|---|---|
| `web/` | Next.js 16 app (Vercel) |
| `packages/shared/` | Shared TS types and helpers |
| `android/` | Kotlin + Jetpack Compose app (outside the Bun workspace) |
| `supabase/` | Migrations and local config (schema = contract) |

## Quick start

Requirements: Bun 1.4+, Docker, Supabase CLI; Android Studio + JDK for Android.

```bash
bun install
supabase start      # prints API URL and anon key; Studio at http://localhost:54323
```

Web:
```bash
cp web/.env.example web/.env.local   # fill NEXT_PUBLIC_SUPABASE_ANON_KEY from `supabase status`
bun dev                              # http://localhost:3000
```

Android:
```bash
cp android/local.properties.example android/local.properties   # set supabase.anonKey
cd android && ./gradlew :app:assembleDebug
```
Run `adb reverse tcp:54321 tcp:54321` so the phone or emulator reaches the host Supabase at `http://127.0.0.1:54321`. Re-run it after reconnecting the device.

## Commands

| Command | Does |
|---|---|
| `bun dev` | Run dev tasks (web on :3000) |
| `bun run dev:web` | Web only |
| `bun run build` | Build all packages |
| `bun run lint` | Lint |
| `bun run typecheck` | Typecheck |
| `supabase start` / `status` / `db reset` | Local DB stack |
| `./gradlew :app:assembleDebug` (in `android/`) | Debug APK |

## Docs

- [AGENTS.md](AGENTS.md): conventions for contributors and AI agents
- [docs/architecture.md](docs/architecture.md)
- [docs/adr/](docs/adr/README.md): architecture decisions
