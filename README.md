# moni

Monorepo base for the finance app:

- `web`: Next.js app (main app for Vercel)
- `mobile`: Expo React Native app
- `packages/shared`: shared TypeScript models

## Requirements

- Node.js 22+
- pnpm 10+

## Run locally

```bash
pnpm install
pnpm dev:web
pnpm dev:mobile
```

You can also run both apps in parallel:

```bash
pnpm dev
```
