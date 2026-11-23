# moni

Monorepo for the finance app:

- `web`: Next.js app (deployed on Vercel)
- `packages/shared`: shared TypeScript models
- `supabase`: database schema and local config
- `mobile`: legacy Expo app, to be replaced by a native Kotlin Android app (not part of the Bun workspace)

## Requirements

- Bun 1.4+
- Docker (only for running Supabase locally)

## Run locally

```bash
bun install
bun dev            # runs the web app at http://localhost:3000
bun run typecheck
bun run lint
bun run build
```

Local database:

```bash
supabase start     # applies migrations, Studio at http://localhost:54323
```
