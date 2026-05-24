# PayFlow — Baseline Specification

> **Current state**: Phases 1–15 complete. Ready for Phase 16+ (specs v3).
> **Full reference spec**: [`PayFlow_Specification_v2.md`](../../PayFlow_Specification_v2.md) (728 lines, sections 1–13)

---

## Reference Documents

| Document | Location |
|----------|----------|
| Full Technical Spec v2.0 | `PayFlow_Specification_v2.md` |
| Original Spec v1.0 | `PayFlow_Specification.docx.txt` |
| Import/Export Guide | `import_export_guide.md` |

---

## 1. Domain Model (Implemented)

### Payment Aggregate (Phase 1)
- State machine: `create()` → PENDING → `capture()` → CAPTURED → `refund()` → REFUNDED/PARTIAL_REFUND
- Transitions: `cancel()` (PENDING→CANCELLED), `expire()` (PENDING→EXPIRED)
- Value Objects: `Money`, `PaymentId`, `MerchantId`, `CardDetails`, `PaymentStatus`
- 100% unit test coverage on domain layer (pure Java, no Spring)

### Merchant Aggregate (Phase 5)
- Fields: MerchantId, name, email, apiKey (BCrypt hashed), createdAt, isActive
- Events: `MerchantCreatedEvent`, `MerchantDeactivatedEvent`

### Webhook Aggregate (Phase 4)
- Fields: WebhookId, MerchantId, url, secret (HMAC key), events, isActive
- Domain rule: HTTPS required, max 5 endpoints per merchant
- Delivery tracking with exponential backoff (5s → 30s → 2m → 10m → 1h, max 5 attempts)

---

## 2. REST API (Implemented)

| Endpoint | Service | Phase |
|----------|---------|-------|
| `POST /v1/payments` | payment-service | 2 |
| `GET /v1/payments/:id` | payment-service | 2 |
| `GET /v1/payments` | payment-service | 2 |
| `POST /v1/payments/:id/capture` | payment-service | 2 |
| `POST /v1/payments/:id/cancel` | payment-service | 2 |
| `POST /v1/payments/:id/refunds` | payment-service | 4 |
| `GET /v1/payments/:id/refunds` | payment-service | 4 |
| `POST /v1/webhooks` | webhook-service | 4 |
| `GET /v1/webhooks` | webhook-service | 4 |
| `DELETE /v1/webhooks/:id` | webhook-service | 4 |
| `GET /v1/webhooks/:id/deliveries` | webhook-service | 4 |
| `POST /v1/merchants` | merchant-service | 5 |
| `GET /v1/merchants/me` | merchant-service | 5 |
| `POST /v1/merchants/me/api-keys` | merchant-service | 5 |
| `DELETE /v1/merchants/me` | merchant-service | 5 |

### Authentication (Phase 8)
- `Authorization: Bearer sk_test_abc123xyz` — unified API key auth across all services

### Error Response Format
```json
{ "error": { "code": "payment_not_found", "message": "...", "param": "id", "requestId": "req_abc" } }
```

---

## 3. Kafka Integration (Implemented — Phase 3)

### Topics
- `payments.events` — partitioned by merchantId
- `merchant.events` — lifecycle events
- `webhook.deliveries` — delivery jobs
- `webhook.dlq` — dead letter queue

### Outbox Pattern
- `outbox_events` table polled by relay → Kafka
- Guarantees at-least-once delivery without distributed transactions

---

## 4. Database Schema (Implemented)

- **Payments schema**: `payments`, `refunds`, `outbox_events` tables
- **Idempotency keys** (Phase 11): `idempotency_keys` table with TTL (24h), SHA-256 body hash, composite index

---

## 5. Frontend (Implemented — Phase 6 + Phase 10)

### Pages
- `/` — Overview (KPI cards, volume chart)
- `/payments` — Paginated table, filter by status/date
- `/payments/new` — Payment creation form (Phase 10)
- `/payments/:id` — Detail with status timeline, refund history
- `/refunds` — Refund form
- `/webhooks` — Endpoint management
- `/settings` — API keys

### State Management
- TanStack Query (server state), Zustand (client state)

---

## 6. Implemented Cross-Cutting Features

| Feature | Phase | Details |
|---------|-------|---------|
| Unified API Key Auth | 8 | Consistent auth filter across all services |
| API Key Rotation Fix | 9 | Zustand/TanStack Query race condition resolved |
| Payment Creation UI | 10 | `/payments/new` form with Zod validation |
| Idempotency Keys | 11 | SHA-256 body hash, 24h TTL, 422 on body mismatch |
| Cross-Service API Key Cache | 12 | Caffeine cache + Kafka invalidation |
| Payment Expiry Scheduler | 13 | Every 5 min, `@Scheduled`, per-item transactions |
| Observability | 14 | Prometheus + Grafana + Zipkin, Micrometer metrics |
| Rate Limiting | 15 | Bucket4j token bucket, per-merchant, endpoint-specific |

---

## 7. Architecture Constraints

- **Hexagonal Architecture**: Domain layer has ZERO framework dependencies
- **Strict TDD**: Every feature starts with a failing test (Red → Green → Refactor)
- **Per-service filter chain**: RequestIdFilter (+0) → Auth (+10) → RateLimit (+15) → Idempotency (+20)
- **API key cache pattern**: Local Caffeine (10min TTL) + Kafka eviction
- **Rate limiting**: Token bucket (100/min default, 20 burst), stricter on sensitive endpoints
