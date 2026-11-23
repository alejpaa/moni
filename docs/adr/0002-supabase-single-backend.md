# ADR-0002: Supabase as single backend, schema as contract

## Status
Accepted

## Date
2026-11-22

## Context
Web and Android need the same data and auth. We want minimal backend code to maintain for a personal app.

## Decision
Supabase (Postgres, Auth, PostgREST) is the only backend. Clients talk to it directly with the anon key; authorization is enforced by Row Level Security (`(select auth.uid()) = user_id`) on every table (`transactions`, `saving_goals`). The SQL in `supabase/migrations/` is the single source of truth and the contract between clients. Changes are new migration files; applied migrations are never edited. `packages/shared` types and Kotlin models in `android/.../data/` mirror the schema by hand.

## Alternatives Considered
- Custom API (Next route handlers or a separate service): more code, a second place for authorization logic.
- Firebase: not relational; weaker fit for finance data and SQL constraints.

## Consequences
- RLS is the security boundary; a table without RLS is a vulnerability.
- Business rules go in DB constraints where possible (e.g. `amount > 0`, non-blank category).
- Type drift between SQL, TS and Kotlin is possible; update all three in the same change. Generated types are a possible future improvement.
- Vendor coupling to Supabase; Postgres itself stays portable.
