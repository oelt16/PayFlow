# PayFlow — Payment Processing Platform
## Full Technical Specification v2.0

> **Status as of v2.0:** Phases 1–15 complete. All planned phases delivered.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Domain Model](#3-domain-model)
4. [REST API Specification](#4-rest-api-specification)
5. [Kafka Event Schema](#5-kafka-event-schema)
6. [Database Schema](#6-database-schema)
7. [Test-Driven Development Strategy](#7-test-driven-development-strategy)
8. [Project Structure](#8-project-structure)
9. [Frontend — React Dashboard](#9-frontend--react-dashboard)
10. [Infrastructure & Deployment](#10-infrastructure--deployment)
11. [Completed Phases (1–9)](#11-completed-phases-19)
12. [Interview Talking Points (Original)](#12-interview-talking-points-original)
13. [Next Phases (10–15)](#13-next-phases-1015)

---

## 1. Project Overview

PayFlow is a simplified payment processing platform inspired by Stripe. It exposes a REST API that allows merchants to create payment intents, capture or cancel them, issue refunds, and receive asynchronous notifications via webhooks. All domain events are published to Kafka, enabling an event-driven architecture that can be extended with downstream consumers such as fraud detection or analytics.

This project is designed as a portfolio piece that demonstrates production-grade engineering practices: Domain-Driven Design with rich aggregates, Test-Driven Development, hexagonal architecture, containerised deployment with Docker and Kubernetes, and real-time event streaming with Apache Kafka.

### 1.1 Goals

- Implement a realistic payment lifecycle: create → capture → refund → webhook notification
- Model the domain using DDD aggregates, value objects, and domain events
- Follow TDD strictly — every feature starts with a failing test
- Publish domain events to Kafka and consume them asynchronously
- Expose a clean REST API consumed by a React/TypeScript frontend dashboard
- Package and deploy all services via Docker Compose and Kubernetes manifests

### 1.2 Out of Scope

- Real card network integration (Visa/Mastercard) — simulated via a mock adapter
- PCI DSS compliance — card data is stubbed; no real PANs are processed
- Multi-currency FX conversion
- OAuth2 / production-grade auth (API key auth is used)

---

## 2. Architecture

PayFlow follows Hexagonal Architecture (also known as Ports and Adapters). The domain layer is completely isolated from frameworks, databases, and messaging infrastructure. All dependencies point inward.

### 2.1 Bounded Contexts

| Bounded Context | Responsibility |
|---|---|
| Payments | Core context. Manages the full payment lifecycle. Contains the Payment aggregate. |
| Merchants | Manages merchant accounts and API key authentication. |
| Webhooks | Manages webhook endpoint registration and delivery with retry logic. |
| Notifications | Kafka consumer that processes domain events and triggers webhook delivery. |

### 2.2 High-Level Component Diagram

```
React Dashboard (TypeScript)
        ↕  REST / HTTPS
NGINX  →  Payment Service  |  Merchant Service  |  Webhook Service
        ↕  Domain Events (Outbox → Kafka)
Apache Kafka  ←→  Notification Service (consumer)
        ↕  JDBC
PostgreSQL (per-service schema, Flyway migrations)
```

In **Docker Compose**, the browser only talks to the frontend container; nginx proxies `/api/*` to the three REST services. In **Kubernetes**, the Ingress routes the same way.

### 2.3 Technology Stack

| Component | Choice & Notes |
|---|---|
| Language (Backend) | Java 21 — virtual threads (Project Loom) for high concurrency |
| Framework | Spring Boot 3.3 · Spring Web MVC · Spring Data JPA |
| Messaging | Apache Kafka 3.9 — domain event streaming |
| Database | PostgreSQL 16 — one schema per bounded context, Flyway migrations |
| Testing | JUnit 5 · Mockito · Testcontainers · WireMock |
| Language (Frontend) | TypeScript 5 · React 18 · Vite |
| UI Components | shadcn/ui · Tailwind CSS · Recharts |
| Containerisation | Docker · Docker Compose (local dev) |
| Orchestration | Kubernetes + Helm charts |
| CI/CD | GitHub Actions — build, test, lint, push to GHCR |
| API Docs | OpenAPI 3.1 via springdoc-openapi |

---

## 3. Domain Model

### 3.1 Payment Aggregate

The Payment aggregate is the core of the system. It enforces all invariants and emits domain events on every state transition.

#### State Machine

| From State | Action | To State | Domain Event Emitted |
|---|---|---|---|
| — | `create()` | PENDING | PaymentCreatedEvent |
| PENDING | `capture()` | CAPTURED | PaymentCapturedEvent |
| PENDING | `cancel()` | CANCELLED | PaymentCancelledEvent |
| CAPTURED | `refund(amount)` | REFUNDED / PARTIAL_REFUND | PaymentRefundedEvent |
| CAPTURED | `expire()` | EXPIRED | PaymentExpiredEvent |

#### Value Objects

- **Money** — amount (BigDecimal) + currency (ISO 4217). Immutable. Validates non-negative amounts.
- **PaymentId** — UUID wrapper. Factory method `PaymentId.generate()`.
- **MerchantId** — UUID wrapper. Validated on construction.
- **CardDetails** — last4, brand (VISA/MASTERCARD/AMEX), expiryMonth, expiryYear. No PANs stored.
- **PaymentStatus** — enum: PENDING, CAPTURED, CANCELLED, REFUNDED, PARTIAL_REFUND, EXPIRED.

### 3.2 Merchant Aggregate

- Fields: MerchantId, name, email, apiKey (BCrypt hashed), createdAt, isActive.
- Domain rule: API key must be hashed with BCrypt before persistence.
- Events: MerchantCreatedEvent, MerchantDeactivatedEvent.

### 3.3 Webhook Aggregate

- Fields: WebhookId, MerchantId, url, secret (HMAC key), events (Set\<EventType\>), isActive.
- Domain rule: URL must be HTTPS. Max 5 endpoints per merchant.
- Delivery tracking: WebhookDelivery entity — attempts, lastAttemptAt, status (PENDING/DELIVERED/FAILED).
- Retry policy: exponential backoff — 5s, 30s, 2m, 10m, 1h. Max 5 attempts.

---

## 4. REST API Specification

### 4.1 Authentication

All API requests must include an API key in the Authorization header:

```
Authorization: Bearer sk_test_abc123xyz
```

### 4.2 Payments Endpoints

| Method + Path | Description |
|---|---|
| `POST /v1/payments` | Create a payment intent. Returns PENDING payment with a client_secret. |
| `GET /v1/payments/:id` | Retrieve a payment by ID. |
| `GET /v1/payments` | List payments. Supports pagination: `?page=0&size=20`. Filter: `?status=CAPTURED`. |
| `POST /v1/payments/:id/capture` | Capture a PENDING payment. Idempotent. |
| `POST /v1/payments/:id/cancel` | Cancel a PENDING payment. |
| `POST /v1/payments/:id/refunds` | Issue a full or partial refund on a CAPTURED payment. |
| `GET /v1/payments/:id/refunds` | List all refunds for a payment. |

#### POST /v1/payments — Request Body

```json
{
  "amount": 10000,
  "currency": "USD",
  "description": "Order #1042",
  "card": {
    "number": "4242424242424242",
    "expMonth": 12,
    "expYear": 2027,
    "cvc": "123"
  },
  "metadata": { "orderId": "ORD-789" }
}
```

### 4.3 Webhooks Endpoints

| Method + Path | Description |
|---|---|
| `POST /v1/webhooks` | Register a new webhook endpoint. |
| `GET /v1/webhooks` | List all webhook endpoints for the authenticated merchant. |
| `DELETE /v1/webhooks/:id` | Deactivate a webhook endpoint. |
| `GET /v1/webhooks/:id/deliveries` | List delivery attempts for a webhook endpoint. |

### 4.4 Merchant Endpoints

| Method + Path | Description |
|---|---|
| `POST /v1/merchants` | Register a new merchant. Returns the plaintext API key (shown once). |
| `GET /v1/merchants/me` | Get the authenticated merchant's profile. |
| `POST /v1/merchants/me/api-keys` | Rotate the API key. Invalidates the previous key immediately. |
| `DELETE /v1/merchants/me` | Deactivate the merchant account. |

### 4.5 Error Response Format

```json
{
  "error": {
    "code": "payment_not_found",
    "message": "No payment found with id: pay_xyz",
    "param": "id",
    "requestId": "req_abc123"
  }
}
```

---

## 5. Kafka Event Schema

### 5.1 Topics

| Topic | Purpose |
|---|---|
| `payments.events` | All domain events from the Payment aggregate. Partitioned by merchantId. |
| `merchant.events` | Merchant lifecycle events (created, deactivated). |
| `webhook.deliveries` | Webhook delivery jobs consumed by the Notification Service. |
| `webhook.dlq` | Dead letter queue — failed webhook deliveries after max retries. |

### 5.2 Event Envelope

```json
{
  "eventId": "evt_01HX...",
  "eventType": "payment.captured",
  "aggregateId": "pay_xyz",
  "merchantId": "mer_abc",
  "occurredAt": "2024-09-01T12:00:00Z",
  "payload": { }
}
```

### 5.3 Event Types

| Event Type | Payload Fields |
|---|---|
| `payment.created` | `{ paymentId, merchantId, amount, currency, status: PENDING }` |
| `payment.captured` | `{ paymentId, merchantId, amount, currency, capturedAt }` |
| `payment.cancelled` | `{ paymentId, merchantId, cancelledAt, reason? }` |
| `payment.refunded` | `{ paymentId, refundId, refundAmount, remainingAmount, isFullRefund }` |
| `payment.expired` | `{ paymentId, merchantId, expiredAt }` |
| `merchant.created` | `{ merchantId, name, email, createdAt }` |
| `merchant.deactivated` | `{ merchantId, deactivatedAt }` |

---

## 6. Database Schema

### 6.1 Payments Schema

```sql
CREATE TABLE payments (
  id            VARCHAR(36)    PRIMARY KEY,
  merchant_id   VARCHAR(36)    NOT NULL,
  amount        NUMERIC(19,2)  NOT NULL,
  currency      CHAR(3)        NOT NULL,
  status        VARCHAR(20)    NOT NULL,
  description   TEXT,
  card_last4    CHAR(4),
  card_brand    VARCHAR(20),
  metadata      JSONB,
  created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  captured_at   TIMESTAMPTZ,
  cancelled_at  TIMESTAMPTZ,
  expires_at    TIMESTAMPTZ
);

CREATE TABLE refunds (
  id            VARCHAR(36)    PRIMARY KEY,
  payment_id    VARCHAR(36)    NOT NULL REFERENCES payments(id),
  amount        NUMERIC(19,2)  NOT NULL,
  reason        TEXT,
  created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
```

### 6.2 Outbox Pattern

To guarantee at-least-once delivery of domain events to Kafka without distributed transactions, PayFlow uses the Transactional Outbox pattern.

```sql
CREATE TABLE outbox_events (
  id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_id  VARCHAR(36)    NOT NULL,
  event_type    VARCHAR(100)   NOT NULL,
  payload       JSONB          NOT NULL,
  created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  published_at  TIMESTAMPTZ,
  published     BOOLEAN        NOT NULL DEFAULT FALSE
);
-- Relay polls: SELECT * FROM outbox_events WHERE published = FALSE ORDER BY created_at LIMIT 100;
```

---

## 7. Test-Driven Development Strategy

### 7.1 Testing Pyramid

| Layer | What to Test |
|---|---|
| Unit Tests (70%) | Domain model: Payment, Merchant, Webhook aggregates and value objects. No Spring context. Pure Java. |
| Integration Tests (20%) | Repository tests with Testcontainers (PostgreSQL). Service layer with real DB, mocked Kafka. |
| API / E2E Tests (10%) | Full Spring Boot context via `@SpringBootTest` + MockMvc. Testcontainers for DB + Kafka. |

### 7.2 TDD Workflow (Red → Green → Refactor)

1. Write a failing test that describes the intended behaviour (Red).
2. Write the minimum production code to make the test pass (Green).
3. Refactor the code while keeping all tests green (Refactor).
4. Commit: test + production code together.

### 7.3 Key Test Scenarios

**Payment Aggregate Unit Tests**
- Given a valid create command, Payment is initialised in PENDING state and emits PaymentCreatedEvent
- Given a PENDING payment, `capture()` transitions to CAPTURED and emits PaymentCapturedEvent
- Given a CAPTURED payment, `capture()` throws `InvalidStateTransitionException`
- Given a CAPTURED payment, `refund(amount > total)` throws `InsufficientRefundableAmountException`
- Given a partial refund, status is PARTIAL_REFUND; given full refund, status is REFUNDED
- Given a PENDING payment older than 1h, `expire()` transitions to EXPIRED

**Money Value Object Unit Tests**
- `Money.of(-1, "USD")` throws `NegativeAmountException`
- `Money.of(10, "XXX")` throws `InvalidCurrencyException`
- Two Money instances with same amount and currency are equal
- `money.add()` and `money.subtract()` produce correct results

**API Integration Tests**
- POST /v1/payments returns 201 with PENDING payment
- POST /v1/payments with invalid currency returns 400 with error code `invalid_currency`
- POST /v1/payments/:id/capture is idempotent — calling twice returns same result
- GET /v1/payments with `?status=CAPTURED` filters correctly and respects pagination
- Unauthenticated request returns 401

---

## 8. Project Structure

```
payflow/
├── backend/
│   ├── payment-service/
│   │   └── src/main/java/com/payflow/payment/
│   │       ├── domain/          # Aggregates, VOs, Events (pure Java)
│   │       ├── application/     # Use cases / command handlers
│   │       ├── infrastructure/  # JPA repos, Kafka producers, adapters
│   │       └── api/             # REST controllers, DTOs, mappers
│   ├── merchant-service/
│   ├── webhook-service/
│   └── notification-service/    # Kafka consumer only
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── services/
│   │   └── types/
│   └── vite.config.ts
├── infra/
│   ├── docker-compose.yml
│   └── k8s/
├── .github/workflows/
│   ├── backend-ci.yml
│   └── frontend-ci.yml
├── .cursor/rules/               # TDD + DDD + Kafka Cursor rules
├── bruno/                       # Bruno API collection
├── development-standards/ai-rules/
└── README.md
```

---

## 9. Frontend — React Dashboard

### 9.1 Pages & Features

| Route | Content |
|---|---|
| `/` — Overview | KPI cards: total volume, transaction count, success rate, refund rate. Volume chart (last 30 days). |
| `/payments` — Transactions | Paginated table of all payments. Filter by status, date range. Click-through to detail. |
| `/payments/:id` — Detail | Full payment detail: status timeline, amount, card info, refund history, raw event log. |
| `/refunds` — New Refund | Form to issue a refund. Full or partial. Confirmation dialog. |
| `/webhooks` — Endpoints | List of registered webhook endpoints. Add/remove. View delivery history per endpoint. |
| `/settings` — API Keys | Show (masked) API key. Rotate key (with race-condition fix from Phase 9). |

### 9.2 State Management

- **TanStack Query** for all server state — caching, refetching, optimistic updates.
- **Zustand** for minimal client state (auth token, UI preferences).
- API key is written to Zustand store **before** TanStack Query cache is invalidated (Phase 9 fix).

### 9.3 Vite Proxy Convention

All frontend calls use the `/api` prefix, stripped by the Vite proxy in dev and by nginx in production:

| Frontend calls | Routes to |
|---|---|
| `/api/v1/payments/*` | payment-service :8081 |
| `/api/v1/merchants/*` | merchant-service :8082 |
| `/api/v1/webhooks/*` | webhook-service :8083 |

---

## 10. Infrastructure & Deployment

### 10.1 Docker Compose (Local Dev)

`infra/docker-compose.yml` starts the full stack:

- PostgreSQL 16 — `payflow` database, Flyway migrations per service
- Apache Kafka 3.9 (KRaft, no Zookeeper)
- Kafka UI — topic browser at `localhost:8080`
- payment-service — port 8081
- merchant-service — port 8082
- webhook-service — port 8083
- notification-service — internal only
- frontend (nginx) — port 3000

### 10.2 Kubernetes Manifests (`infra/k8s/`)

| Resource | Notes |
|---|---|
| Namespace | `payflow` |
| Deployment | One per service. 2 replicas. Resource limits defined. |
| Service | ClusterIP internal; LoadBalancer for API Gateway. |
| ConfigMap | Kafka brokers, DB host, topic names. |
| Secret | DB credentials, API keys. |
| HPA | payment-service: scale 2→10 pods on CPU > 60%. |
| PVC | PostgreSQL data volume. |
| Ingress | NGINX: `payflow.local` → frontend (nginx inside pod proxies `/api`). |

### 10.3 GitHub Actions CI/CD

**backend-ci.yml** — `mvnw verify` (Testcontainers via Docker socket). On push to `main`: build + push 4 images to GHCR.

**frontend-ci.yml** — lint, test, build. On push to `main`: push frontend image to GHCR.

Images: `ghcr.io/<owner>/payflow/<service>:latest` and `:sha-<short>`.

---

## 11. Completed Phases (1–9)

| Phase | Deliverable | Status |
|---|---|---|
| Phase 1 — Domain Core | Payment aggregate, all value objects, 100% unit test coverage. No Spring, no DB. | ✅ Done |
| Phase 2 — Payment Service | Spring Boot app. JPA repositories. REST API: create/capture/cancel. Outbox table. Testcontainers. | ✅ Done |
| Phase 3 — Kafka Integration | Outbox relay → Kafka. Notification Service consumer. | ✅ Done |
| Phase 4 — Refunds & Webhooks | Refund use case. Webhook service with HMAC delivery and exponential backoff retry. | ✅ Done |
| Phase 5 — Merchant Service | Merchant CRUD. BCrypt API key hashing. Merchant context Kafka events. | ✅ Done |
| Phase 6 — React Frontend | All pages, TanStack Query, Recharts, Zod forms, `/api` proxy convention. | ✅ Done |
| Phase 7 — Infra & Polish | Docker Compose, K8s manifests, GitHub Actions CI, README with architecture diagram. | ✅ Done |
| Phase 8 — Unified API Key Auth | Consistent API key validation across all services. | ✅ Done |
| Phase 9 — API Key Rotation Fix | Fixed TanStack Query/Zustand race condition on rotate. Fixed `clientSecret: null` Zod parse failure. | ✅ Done |
| Phase 14 — Observability | Prometheus metrics, Grafana dashboard, Zipkin tracing. | ✅ Done |
| Phase 15 — Rate Limiting | Token bucket per API key (Bucket4j + Caffeine), 429 with headers, stricter endpoint limits. | ✅ Done |

---

## 12. Interview Talking Points (Original)

- **Why DDD?** The payment domain has complex invariants and a clear ubiquitous language. DDD forced modelling of state transitions explicitly in the aggregate rather than in scattered service methods.
- **Why the Outbox Pattern?** Dual writes (DB + Kafka in one transaction) are unsafe. The outbox guarantees exactly-once write to the DB and at-least-once publish to Kafka, which is the right trade-off.
- **Why Hexagonal Architecture?** The domain layer has zero framework dependencies. All business logic can be tested without starting Spring or Kafka — tests are fast and deterministic.
- **Why TDD?** For a payment system, correctness is critical. Writing tests first forced API design before implementation and gave confidence to refactor the state machine.
- **Why Kafka over direct HTTP webhooks?** Decoupling the event producer from webhook delivery allows independent scaling and retry without blocking the payment flow. The DLQ captures permanently failed deliveries for ops visibility.
- **Why Java 21 virtual threads?** Virtual threads allow high concurrency with a simple blocking I/O model, avoiding the complexity of reactive programming (WebFlux) while still handling thousands of concurrent requests.

---

## 13. Next Phases (10–15)

The following phases extend PayFlow from a spec-complete portfolio project into a demonstrably production-aware platform. Each phase is self-contained and independently deployable.

---

### Phase 10 — Payment Creation in the UI (~3 days)

**Why first?** The README explicitly notes that payment creation is not available from the UI. This is the most visible gap to anyone evaluating the project. Close it before adding anything else.

**Deliverables:**

- New `/payments/new` route in the frontend.
- `CreatePaymentForm` component: amount, currency selector, simulated card fields (number, expiry, CVC). Zod validation matching the backend schema.
- Zod schema: `amount` is a positive integer (cents), `currency` is one of `["USD", "EUR", "GBP"]`, card number must be exactly 16 digits.
- On success: redirect to `/payments/:id` detail page and show the new PENDING payment.
- React Query mutation using `useMutation`, with optimistic list update in the payments cache.
- TDD: add `CreatePaymentForm.test.tsx` covering validation errors, submit success, and API error states.
- Update README to remove the "payment creation not available from UI" note.

**New API client method:**

```typescript
// src/services/payments.ts
export const createPayment = (req: CreatePaymentRequest): Promise<PaymentResponse> =>
  apiClient.post('/api/v1/payments', req).then(r => paymentResponseSchema.parse(r.data));
```

---

### Phase 11 — Idempotency Keys (~1 week)

**Why?** Idempotency keys are a fundamental primitive of every production payment API (Stripe, Adyen, Braintree all require them). A client can safely retry a `POST /v1/payments` without risking duplicate charges. This is one of the highest-signal features you can add for fintech hiring.

**Domain rule:** An idempotency key is a client-supplied string (UUID format, 64 chars max). If payment-service receives a request with a key it has already processed, it returns the original response verbatim without re-executing the use case.

**Backend deliverables:**

- New `idempotency_keys` table in the payments schema:

```sql
CREATE TABLE idempotency_keys (
  key           VARCHAR(64)    PRIMARY KEY,
  merchant_id   VARCHAR(36)    NOT NULL,
  request_hash  VARCHAR(64)    NOT NULL,  -- SHA-256 of request body
  response_body JSONB          NOT NULL,
  http_status   INT            NOT NULL,
  created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  expires_at    TIMESTAMPTZ    NOT NULL   -- TTL: 24h
);
```

- `IdempotencyFilter` (servlet filter): extract `Idempotency-Key` header. If key exists and `merchant_id` + `request_hash` match → return cached response. If key exists but body differs → return `422 idempotency_key_reuse`. If new → proceed and store result after execution.
- Applies only to `POST /v1/payments`, `POST /v1/payments/:id/capture`, `POST /v1/payments/:id/refunds`.
- A scheduled job (`@Scheduled`) purges expired keys daily.
- TDD: unit tests on the filter logic; integration test proving a duplicate POST returns the same `paymentId`.

**Interview talking point:** "The filter stores the SHA-256 of the request body alongside the key. If the same key is replayed with a different body, we return 422 — this prevents a subtle class of bugs where a client accidentally reuses a key for a different operation."

---

### Phase 12 — Cross-Service API Key Cache (~1 week)

**Why?** This was explicitly deferred from Phase 5. Currently payment-service and webhook-service validate keys against a static YAML list. This is a known gap. Completing it closes the single-source-of-truth story and adds a meaningful distributed systems pattern.

**Pattern:** Local read-through cache with Kafka-based invalidation.

**Backend deliverables (payment-service and webhook-service):**

- Add Caffeine dependency to both services.
- `MerchantApiKeyCache`: on cache miss, calls `GET /v1/internal/merchants/validate-key` on merchant-service (HTTP, Feign or RestClient). Caches the result (valid/invalid + merchantId) with a 5-minute TTL.
- On `MerchantDeactivatedEvent` from `merchant.events` topic: evict the deactivated merchant's key from the cache immediately.
- `payflow.security.api-keys` static config is removed from payment-service and webhook-service YAML.
- New internal endpoint on merchant-service: `POST /v1/internal/merchants/validate-key` — takes the raw key, BCrypt-matches against DB, returns merchantId or 404. Not exposed via nginx (ClusterIP only in K8s).

```
Request → payment-service auth filter
  → check Caffeine cache (hit? return merchantId)
  → cache miss: call merchant-service /internal/validate-key
  → cache result for 5 min
  → MerchantDeactivatedEvent consumed → evict entry
```

- TDD: test that a deactivated merchant is rejected within one Kafka poll cycle after the event; test that a cache hit does not call merchant-service.

**Interview talking point:** "The 5-minute TTL means a compromised key is valid for at most 5 minutes if deactivation arrives after a cache population. The Kafka event cuts that window to near-zero in the normal case. This is the same pattern Stripe uses for their internal service mesh."

---

### Phase 13 — Payment Expiry Scheduler (~3 days)

**Why?** The domain model has an `expire()` method and a `payment.expired` event type defined since Phase 1. The scheduler that actually calls it has never been built. Without it, PENDING payments accumulate indefinitely — which would be a clear gap to any technical reviewer.

**Backend deliverables (payment-service):**

- `PaymentExpiryScheduler`: `@Scheduled(fixedDelay = 60_000)` job that queries for PENDING payments where `created_at < NOW() - INTERVAL '1 hour'`.
- For each, calls `payment.expire()` on the aggregate, persists the new EXPIRED status, and writes `PaymentExpiredEvent` to the outbox table — exactly like any other domain event.
- This triggers the same notification pipeline: outbox relay → Kafka → notification-service → webhook delivery to the merchant.
- TDD: integration test with Testcontainers — seed a PENDING payment with `created_at` set to 2 hours ago, trigger the scheduler manually, assert status is EXPIRED and outbox contains the event.

**DB query:**

```sql
SELECT * FROM payments
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL '1 hour'
  AND expires_at IS NULL
LIMIT 100;
```

**Note on correctness:** Process in batches of 100 to avoid a long transaction. Each payment is expired in its own transaction so a single failure does not roll back the batch.

---

### Phase 14 — Observability: Metrics + Tracing (~1 week)

**Why?** Every fintech engineering interview at a bank or neobank will ask "how do you monitor this in production?". Adding real Prometheus metrics and a Grafana dashboard transforms the answer from theoretical to demonstrable. This is the single highest-impact addition for senior-level interviews.

**Deliverables:**

#### 14.1 Custom Micrometer Metrics (all backend services)

Add to payment-service:

```java
// Application-level counters and timers
Counter.builder("payflow.payment.created").tag("currency", currency).register(registry);
Counter.builder("payflow.payment.captured").register(registry);
Counter.builder("payflow.payment.failed").tag("reason", reason).register(registry);
DistributionSummary.builder("payflow.payment.amount")
    .tag("currency", currency)
    .baseUnit("cents")
    .register(registry);
Timer.builder("payflow.payment.capture.latency").register(registry);
```

Add to webhook-service:

```java
Counter.builder("payflow.webhook.delivery.attempted").register(registry);
Counter.builder("payflow.webhook.delivery.succeeded").register(registry);
Counter.builder("payflow.webhook.delivery.failed").register(registry);
Gauge.builder("payflow.webhook.dlq.size", dlqRepository, DlqRepository::countPending).register(registry);
```

Add to outbox relay (all services):

```java
Gauge.builder("payflow.outbox.pending", outboxRepository, r -> r.countByPublished(false)).register(registry);
```

#### 14.2 Spring Boot Actuator + Prometheus

Enable in all services:

```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  metrics.export.prometheus.enabled: true
```

#### 14.3 Prometheus + Grafana in Docker Compose

Add to `infra/docker-compose.yml`:

```yaml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports: ["9090:9090"]

grafana:
  image: grafana/grafana:latest
  ports: ["3001:3000"]
  volumes:
    - ./grafana/dashboards:/var/lib/grafana/dashboards
    - ./grafana/provisioning:/etc/grafana/provisioning
```

`infra/prometheus.yml` scrape config for all four services.

#### 14.4 Pre-configured Grafana Dashboard

Provide `infra/grafana/dashboards/payflow.json` with panels for:

- Payments created per minute (counter rate)
- Payment success rate (captured / created %)
- Captured volume in USD/EUR/GBP
- P50/P95/P99 capture latency
- Webhook delivery success rate
- Outbox pending depth (alert threshold: >100 for 5 minutes)

#### 14.5 Distributed Tracing (optional but recommended)

Add `spring-boot-starter-actuator` + Micrometer Tracing with Zipkin exporter. Add Zipkin to Docker Compose. Every payment request gets a `traceId` that spans payment-service → Kafka → notification-service → webhook-service, visible in Zipkin's UI.

**Interview talking point:** "The outbox pending gauge is the most important operational metric. If it grows, either Kafka is down or the relay is stuck. I set an alert threshold so on-call knows within 5 minutes if events stop flowing."

---

### Phase 15 — Rate Limiting (~3 days)

**Why?** Rate limiting is a table-stakes feature for any public payment API. It protects the platform from abuse, demonstrates security awareness, and is a common interview topic at banks and fintech companies.

**Pattern:** Token bucket per API key, enforced at the Spring filter layer using Bucket4j with an in-memory backend (Caffeine). No Redis dependency — keep it simple.

**Backend deliverables (payment-service and merchant-service):**

- Add `bucket4j-core` dependency.
- `RateLimitFilter` (servlet filter, runs after auth): looks up or creates a `Bucket` for the authenticated `merchantId`. Each bucket: 100 requests/minute with a burst of 20.
- On bucket exhausted: return `429 Too Many Requests` with headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1725192060
Retry-After: 45
```

- Rate limit configuration is externalised to `application.yml`:

```yaml
payflow:
  rate-limit:
    requests-per-minute: 100
    burst-capacity: 20
```

- Stricter limits on sensitive endpoints: `POST /v1/payments` — 20/minute, `POST /v1/merchants/me/api-keys` — 3/hour.
- TDD: integration test verifying that the 21st request within a minute returns 429; verify that the `Retry-After` header value is positive.

**Interview talking point:** "I chose token bucket over fixed window because it handles burst traffic gracefully. A merchant can do 20 rapid requests then gets smoothed to 100/minute. The stricter limit on `/v1/payments` protects against card-testing attacks — a common fraud vector."

---

## Suggested Implementation Order for Phases 10–15

| Priority | Phase | Effort | Why this order |
|---|---|---|---|
| 1 | Phase 10 — Payment creation UI | 3 days | Closes the most visible gap immediately |
| 2 | Phase 13 — Expiry scheduler | 3 days | Completes a domain feature defined since Phase 1; low risk |
| 3 | Phase 11 — Idempotency keys | 1 week | Highest-signal fintech pattern; backend-only |
| 4 | Phase 12 — API key cache | 1 week | Completes the deferred Phase 5 architecture decision |
| 5 | Phase 14 — Observability | 1 week | Transforms interview answers from theoretical to demonstrable |
| 6 | Phase 15 — Rate limiting | 3 days | Fast to implement once 11–14 are done; good final polish |

---

## New Interview Talking Points (Phases 10–15)

- **Idempotency (Phase 11):** "Payment APIs must be safe to retry. I implemented idempotency keys with a SHA-256 body hash. The same key with a different body returns 422 — this prevents a subtle class of bugs where a client reuses a key for a different payment."
- **Distributed cache invalidation (Phase 12):** "The local cache has a 5-minute TTL, but MerchantDeactivatedEvent from Kafka cuts the deactivation propagation time to near-zero. This is the read-your-writes trade-off — eventual consistency with a bounded inconsistency window."
- **Expiry scheduler (Phase 13):** "I process expired payments in batches of 100, each in its own transaction. A single failure doesn't roll back the whole batch. The expiry flows through the same outbox → Kafka → webhook pipeline as any other domain event — no special casing."
- **Observability (Phase 14):** "The outbox pending gauge is the single most important operational metric. If it grows indefinitely, events stop flowing. I pre-configured a Grafana alert at >100 pending for 5 minutes, which gives on-call 5 minutes to react before any SLA breach."
- **Rate limiting (Phase 15):** "Token bucket handles bursts better than fixed window. The stricter limit on POST /v1/payments specifically protects against card-testing attacks — attackers probe whether card numbers are valid by submitting many small payments rapidly."

---

*PayFlow — Full Technical Specification v2.0*
