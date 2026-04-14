# PayFlow frontend

Merchant dashboard for PayFlow: payments, refunds, webhooks, and API key settings.

## Requirements

- **Node.js 20+** (repo [`.nvmrc`](../.nvmrc))

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Vite dev server (default port 5173) with `/api` → backend proxy |
| `npm run build` | Typecheck + production bundle |
| `npm run preview` | Preview production build |
| `npm run test` | Vitest (jsdom) |
| `npm run lint` | ESLint |

## Local API

The app calls **`/api` only** (no host or ports in source). With `npm run dev`, [`vite.config.ts`](vite.config.ts) proxies:

- `/api/v1/payments` → `http://localhost:8081`
- `/api/v1/merchants` → `http://localhost:8082`
- `/api/v1/webhooks` → `http://localhost:8083`

Start those services locally (and Postgres) before using the dashboard.

## Stack

React 19, TypeScript, Vite, TanStack Query, Zustand, Zod, axios, Tailwind v4, shadcn/ui, Recharts, React Router.

See [PHASE6_REACT_FRONTEND.md](../PHASE6_REACT_FRONTEND.md) for phase scope and auth notes.
