# ADR-0001: Monorepo with Bun workspace + Turborepo, Android outside

## Status
Accepted

## Date
2026-11-22

## Context
The project has a web app, shared TypeScript code, a Supabase schema and a mobile client. They change together (schema, types, UI), so one repo is convenient. The Android client is built with Gradle, not a JS toolchain.

## Decision
One git repo. JS packages live in a Bun workspace (`web`, `packages/*`) orchestrated by Turborepo (`build`, `dev`, `lint`, `typecheck`). `packages/shared` is consumed as TS source via Next's `transpilePackages` (no build step). `android/` and `supabase/` are in the repo but not workspace members.

## Alternatives Considered
- Separate repos per client: harder to keep schema and types in sync.
- Putting Android in the JS workspace: Gradle does not use it; adds no value.
- No Turborepo: simpler, but we want cached, uniform task running across packages.

## Consequences
- One PR can change schema, web and Android together.
- Android is not covered by `bun run build/lint/typecheck`; it is built separately with Gradle.
- Shared TS code cannot be consumed by Kotlin; models are duplicated and kept in sync manually (see ADR-0002).
