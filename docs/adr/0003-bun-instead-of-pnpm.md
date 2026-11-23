# ADR-0003: Bun instead of pnpm

## Status
Accepted

## Date
2026-11-22

## Context
The workspace used pnpm. We wanted a single fast tool for installs and running scripts.

## Decision
Use Bun 1.4 as package manager and script runner (`packageManager: bun@1.4.4`, `bun.lock`, root `workspaces`). Turborepo stays as the task runner. Next.js itself still runs on its Node-based toolchain via the package scripts.

## Alternatives Considered
- Stay on pnpm: works, but no reason to keep two tools.
- npm/yarn: slower, no advantage.

## Consequences
- Use `bun install` / `bun run`; do not add `pnpm-lock.yaml` or `package-lock.json`.
- Vercel must detect Bun via `bun.lock`.
- Contributors need Bun installed.
