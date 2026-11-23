# ADR-0005: Web auth with @supabase/ssr, Next 16 proxy and server actions

## Status
Accepted

## Date
2026-11-22

## Context
The web app needs Supabase sessions that work in server components and survive refreshes. Next.js 16 renamed `middleware` to `proxy`.

## Decision
- `@supabase/ssr` with cookie-based sessions; server client in `web/src/lib/supabase/server.ts`.
- `web/src/proxy.ts` refreshes the session on each request and redirects signed-out users to `/login` (and signed-in users away from it). It is an optimistic check only: pages still call `getUser()` and RLS enforces access.
- Mutations and login/logout use server actions (`web/src/app/actions.ts`, `web/src/app/login/actions.ts`); the dashboard is `web/src/app/page.tsx`.
- Env: `web/.env.local` from `web/.env.example`.

## Alternatives Considered
- Client-side `supabase-js` with localStorage: no SSR-aware session, flicker on load.
- Route handlers for mutations: more boilerplate than server actions.
- Auth.js: redundant with Supabase Auth.

## Consequences
- Use `getUser()` (verifies with Auth server) rather than `getSession()` for authorization decisions on the server.
- Next 16 APIs differ from older docs; consult `node_modules/next/dist/docs/` (see `web/AGENTS.md`).
- Android uses its own supabase-kt session; both rely on the same RLS.
