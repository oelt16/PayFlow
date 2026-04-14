# Phase 6: React frontend dashboard

This document describes what Phase 6 added to PayFlow, how to test and run it, deployment options (with and without Docker), a short **tutorial for the dashboard**, and how the **whole system** fits together. The canonical spec remains [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) (section 9: frontend).

---

## 1. What this phase implemented

Phase 6 delivers the **merchant dashboard** under [`frontend/`](frontend/).

### Stack

- React 19, TypeScript 5, Vite 8  
- TanStack Query (server state), Zustand with `localStorage` persistence (API key), Zod, axios  
- Tailwind CSS v4, shadcn/ui, Recharts  
- React Router, Vitest + Testing Library + `@testing-library/jest-dom`

### API contract from the browser

Every request uses **`API_BASE = '/api'`** only (no hard-coded host or ports in application code). That matches the future Kubernetes pattern: Ingress serves the SPA and routes `/api/*` to backends.

In **local dev**, Vite rewrites and proxies (see [`frontend/vite.config.ts`](frontend/vite.config.ts)):

| Browser calls | Proxied to | Backend |
|----------------|------------|---------|
| `/api/v1/payments/*` | `http://localhost:8081/v1/payments/*` | payment-service |
| `/api/v1/merchants/*` | `http://localhost:8082/v1/merchants/*` | merchant-service |
| `/api/v1/webhooks/*` | `http://localhost:8083/v1/webhooks/*` | webhook-service |

### Dashboard routes

| Path | Purpose |
|------|---------|
| `/` | Overview: KPI cards + 30-day volume chart |
| `/payments` | Paginated table, status filter, links to detail |
| `/payments/:id` | Timeline, card stub, capture / cancel / link to refund |
| `/refunds` | Refund form (Zod + confirmation); query `?paymentId=&currency=` |
| `/webhooks` | Register HTTPS endpoints, list, delete, delivery log (polls every 5s) |
| `/settings` | Enter or register API key, merchant profile, rotate key |
| `*` | Not found |

Routes other than `/settings` (and the catch-all) are **protected**: if no API key is stored, the app sends you to **Settings**.

### Important: API keys across three services

- **merchant-service** issues and validates **BCrypt-hashed** keys (`POST /v1/merchants`, `GET /v1/merchants/me`, etc.).  
- **payment-service** and **webhook-service** still use **static** keys from `payflow.security.api-keys` in their `application.yml` unless you change them.

For the smoothest local demo, use the dev key already configured on payment and webhook (for example `sk_test_dev` with merchant id `mer_test_dev`), **or** register via the dashboard and add a matching entry to those YAML files. Details: [PHASE5_MERCHANT_SERVICE.md](PHASE5_MERCHANT_SERVICE.md).

### Key files

| Area | Location |
|------|----------|
| Router + query client | [`frontend/src/App.tsx`](frontend/src/App.tsx) |
| Layout, sidebar | [`frontend/src/components/layout/`](frontend/src/components/layout/) |
| Auth gate | [`frontend/src/components/RequireAuth.tsx`](frontend/src/components/RequireAuth.tsx) |
| HTTP client + Bearer header | [`frontend/src/services/api-client.ts`](frontend/src/services/api-client.ts) |
| REST wrappers + Zod parsing | [`frontend/src/services/payments.ts`](frontend/src/services/payments.ts), [`webhooks.ts`](frontend/src/services/webhooks.ts), [`merchants.ts`](frontend/src/services/merchants.ts) |
| Pages | [`frontend/src/pages/`](frontend/src/pages/) |

---

## 2. How to test

### Frontend (no Docker required)

Requires **Node.js 20+** ([`.nvmrc`](.nvmrc)).

```bash
cd frontend
npm ci
npm run lint
npm run test
npm run build
```

- **Vitest** uses [`frontend/vitest.config.ts`](frontend/vitest.config.ts) with the React plugin only so tests do not load the Tailwind Vite native addon.  
- **Production build** uses full Tailwind via [`frontend/vite.config.ts`](frontend/vite.config.ts).

### Backend (optional, full platform)

From the repo root:

```bash
cd backend
./mvnw verify
```

With **Docker** running, Testcontainers can start Postgres and Kafka for integration tests. Without Docker, many integration tests are skipped; domain and unit tests still run.

### Manual smoke test (browser)

1. Start **PostgreSQL** (database/user matching each service `application.yml`, often `payflow` / `payflow`).  
2. Start **Kafka** on `localhost:9092` if you rely on outbox publishing (not strictly required to click through read-only UI if APIs respond).  
3. Start each Spring Boot service on **8081**, **8082**, **8083** (see module READMEs under `backend/`).  
4. Run `npm run dev` in `frontend/` and open the URL Vite prints (typically `http://localhost:5173`).  
5. On **Settings**, save an API key that your running payment and webhook services accept, or register a merchant and align static keys on those services.

---

## 3. How to run and deploy

### Without Docker (processes on your machine)

**Frontend**

```bash
cd frontend
npm ci
npm run dev          # development, with Vite proxy
# or
npm run build && npm run preview   # production-like static assets + preview server
```

**Backends** (typical pattern; ports from each `application.yml`):

```bash
cd backend
./mvnw -pl payment-service spring-boot:run
./mvnw -pl merchant-service spring-boot:run
./mvnw -pl webhook-service spring-boot:run
# optional: notification-service consumer
```

Ensure **JDBC URLs** and **Kafka bootstrap servers** point at your local Postgres and broker (or overrides via environment variables such as `SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`).

**Production-shaped frontend without Vite proxy**

Build static files with `npm run build` and serve `frontend/dist/` behind any HTTP server. You must also provide **reverse proxy** rules so browser requests to `/api/v1/...` reach the correct service (same mapping as the Vite `server.proxy` table). Phase 7 is expected to add NGINX or Ingress manifests for that.

### With Docker

**Current repo state:** [`infra/docker-compose.yml`](infra/docker-compose.yml) is a **placeholder** (Phase 7 in the spec). It does not yet start Postgres, Kafka, or the Java services. You can still use Docker **à la carte**:

- Run **official Postgres 16** and **Kafka** images, publish ports **5432** and **9092** to the host, and keep `application.yml` defaults, **or**  
- Run **Testcontainers** implicitly via `./mvnw verify` when Docker is available.

A full **one-command Compose stack** for app + DB + broker + UI is planned with Phase 7; until then, document your own `docker compose` file or run jars/images you build from each module’s Dockerfile when you add them.

---

## 4. Tutorial: using the frontend

### First visit

1. Open the app (Vite dev URL or your deployed origin).  
2. You are sent to **Settings** if no key is saved.

### Connect with an existing key

1. Paste your `sk_test_…` key into **API key** and click **Save key**.  
2. Open **Overview** or **Payments** from the sidebar.  
3. If payment-service rejects the key, you see errors in toasts or empty/error states; fix `payflow.security.api-keys` on payment-service (and webhook-service for webhooks) or use the dev key those files already define.

### Register a new merchant

1. On **Settings**, fill **Name** and **Email**, click **Register and save key**.  
2. The new raw key is stored automatically.  
3. Use **Merchant profile** to confirm `GET /v1/merchants/me` works.  
4. For **Payments** and **Webhooks**, configure payment-service and webhook-service static keys to match this merchant and key, or continue using separate dev keys for those services (see Phase 5 doc).

### Day-to-day flows

- **Overview:** quick KPIs and a 30-day volume chart from sampled payment list data.  
- **Payments:** filter by status, paginate, click an id for **detail**.  
- **Payment detail:** **Capture** or **Cancel** when status is `PENDING`; **Refund** links to the refund form with query params prefilled when status allows.  
- **Refunds:** enter payment id, amount in **minor units** (e.g. cents), ISO currency, optional reason; confirm in the dialog.  
- **Webhooks:** register an **HTTPS** URL and select event types; expand **Deliveries** to see attempts (auto-refresh every 5 seconds).  
- **Rotate API key:** on Settings, **Rotate API key** returns a new secret once; update any other services or clients that still used the old key.

---

## 5. How the whole system works

PayFlow is a **multi-service** payment platform with **hexagonal** Java services and a **React** dashboard.

### Bounded contexts (backend)

| Service | Role | Default port |
|---------|------|--------------|
| **payment-service** | Payment aggregate, REST for create/capture/cancel/refund, outbox → `payments.events` | 8081 |
| **merchant-service** | Merchants, BCrypt API keys, outbox → `merchant.events` | 8082 |
| **webhook-service** | Webhook endpoints, HMAC delivery, retries, DLQ | 8083 |
| **notification-service** | Consumes Kafka (e.g. payment events), triggers webhook dispatch | (no public REST) |

Each service uses its own **Postgres schema** (Flyway). Domain changes are written to the DB and **transactional outbox** rows; schedulers publish to **Kafka** so HTTP paths stay fast.

### Request path (dashboard → payment)

1. The browser calls `GET /api/v1/payments` (relative URL).  
2. **Vite** (dev) or **Ingress** (prod) forwards to payment-service as `GET /v1/payments`.  
3. **ApiKeyAuthenticationFilter** resolves the merchant from `Authorization: Bearer …`.  
4. Application layer loads data from JPA adapters and returns JSON.  
5. The frontend **axios** instance attaches the Bearer token from **Zustand** on every request.

### Event path (payment → webhook)

1. Payment aggregate appends domain events to the **outbox** in the same transaction as state changes.  
2. **Outbox relay** publishes to Kafka (`payments.events`).  
3. **notification-service** (or similar) consumes events and calls webhook-service internal APIs or enqueue delivery.  
4. **webhook-service** signs payloads and POSTs to merchant URLs, with retries and DLQ as in the spec.

### Frontend’s place

The SPA is **stateless** regarding credentials except for the key in **localStorage**. All server truth comes from REST + TanStack Query caches. The `/api` prefix keeps the same built assets valid behind a reverse proxy in staging or production.

---

## Related links

- [frontend/README.md](frontend/README.md) — short frontend readme  
- [PHASE5_MERCHANT_SERVICE.md](PHASE5_MERCHANT_SERVICE.md) — merchant API and key model  
- [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) — full specification  
