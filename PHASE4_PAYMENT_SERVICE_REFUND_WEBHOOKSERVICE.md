# Phase 4: Refunds and webhooks

This document describes what Phase 4 added to PayFlow, how the pieces connect, and how to run tests and local deployments with or without Docker.

For the full platform specification, see [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt).

---

## What shipped in Phase 4

### Payment service (refunds)

- **REST**
  - `POST /v1/payments/{id}/refunds`: create a refund (amount in **minor units**, currency, optional reason).
  - `GET /v1/payments/{id}/refunds`: list refunds for a payment.
- **Persistence**: `payments.refunds` table (Flyway `V2__refunds_table.sql`).
- **Domain events**: `payment.refunded` published via the existing transactional outbox to Kafka topic `payments.events`. The outbox payload includes `merchantId` so the relay can use it as the Kafka message key (same idea as other payment events).
- **API responses**: `PaymentResponse` includes `totalRefunded` and `amountRefunded` (both in minor units; today they mirror the same running total).

Default HTTP port: **8081**.

### Webhook service

- **REST (API key, same style as payment-service)**
  - `POST /v1/webhooks`: register an HTTPS URL and event types; response includes a **secret** (shown once).
  - `GET /v1/webhooks`: list endpoints for the authenticated merchant.
  - `DELETE /v1/webhooks/{id}`: deactivate an endpoint.
  - `GET /v1/webhooks/{id}/deliveries`: delivery history.
- **Internal (no API key; intended for cluster / trusted callers)**
  - `POST /internal/webhooks/dispatch`: body: `merchantId`, `eventType`, `eventPayload` (JSON). Finds matching active endpoints, creates deliveries, POSTs to merchant URLs with **HMAC-SHA256** over the body in the `Payflow-Signature` header (hex).
- **Retries**: up to **5** attempts with delays **5s, 30s, 2m, 10m, 1h** between failures; permanently failed deliveries are published to Kafka topic **`webhook.dlq`** when Kafka is configured.

Default HTTP port: **8083**. Database schema: **`webhooks`**.

### Notification service

- After consuming a message from **`payments.events`**, it calls webhook-service:

  `POST {payflow.webhook-dispatch.base-url}/internal/webhooks/dispatch`

- Configure with `payflow.webhook-dispatch.base-url` and `payflow.webhook-dispatch.enabled` (see [backend/notification-service/src/main/resources/application.yml](backend/notification-service/src/main/resources/application.yml)).

Default HTTP port: **8084**.

### End-to-end flow (happy path)

1. Payment service writes business data and outbox rows; **OutboxRelay** publishes to Kafka.
2. Notification service consumes the envelope JSON and forwards dispatch to webhook-service.
3. Webhook-service delivers to the merchant URL with HMAC signing and records delivery status.

---

## How to test

### Backend unit and integration tests (Maven)

From the repo root:

```bash
cd backend
./mvnw verify
```

- **Without Docker**: JVM-only tests run; any test using **Testcontainers** (PostgreSQL, Kafka) is skipped when Docker is unavailable (`disabledWithoutDocker = true` on those classes). You still get value from pure unit tests and slices that do not start containers.
- **With Docker**: start Docker Desktop (or a compatible engine), then run the same command. Integration tests spin up real Postgres and/or Kafka containers and exercise refunds, outbox/Kafka, webhooks, and notification-to-webhook HTTP dispatch (WireMock stubs webhook-service from the notification module's perspective).

Target a single module while iterating:

```bash
./mvnw -pl payment-service verify
./mvnw -pl webhook-service verify
./mvnw -pl notification-service verify
```

### Manual API smoke checks (services already running)

Prerequisites: Postgres with database/user matching each service `application.yml`, Kafka on `localhost:9092`, and services started in an order that satisfies dependencies (Postgres → Kafka → payment & webhook → notification).

Example API key (dev, payment and webhook services):

```http
Authorization: Bearer sk_test_dev
```

**Refund (after creating and capturing a payment):**

```bash
# Create payment (see payment-service OpenAPI or existing integration tests for body shape)
# Capture: POST /v1/payments/{id}/capture
# Refund:
curl -sS -X POST "http://localhost:8081/v1/payments/PAYMENT_ID/refunds" \
  -H "Authorization: Bearer sk_test_dev" \
  -H "Content-Type: application/json" \
  -d '{"amount":1000,"currency":"USD","reason":"Customer request"}'

curl -sS "http://localhost:8081/v1/payments/PAYMENT_ID/refunds" \
  -H "Authorization: Bearer sk_test_dev"
```

**Webhook registration and deliveries:**

```bash
curl -sS -X POST "http://localhost:8083/v1/webhooks" \
  -H "Authorization: Bearer sk_test_dev" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/webhook","events":["payment.created"]}'

curl -sS "http://localhost:8083/v1/webhooks" \
  -H "Authorization: Bearer sk_test_dev"
```

Dispatch is normally triggered by Kafka via notification-service; you can still call internal dispatch locally for debugging (no auth):

```bash
curl -sS -X POST "http://localhost:8083/internal/webhooks/dispatch" \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"mer_test_dev","eventType":"payment.created","eventPayload":{"demo":true}}'
```

---

## How to run and deploy locally

### Without Docker (processes on your machine)

1. **PostgreSQL 16**: create database and user (e.g. `payflow` / `payflow` as in the YAML files). Ensure the **`payments`** and **`webhooks`** schemas exist or rely on Flyway (`create-schemas` / migrations) as configured per service.
2. **Apache Kafka**: broker reachable at `localhost:9092` (or override `spring.kafka.bootstrap-servers` in each service).
3. **Start services** (separate terminals or your IDE), recommended order:
   - `payment-service` (8081)
   - `webhook-service` (8083)
   - `notification-service` (8084)
4. **Align URLs**: notification-service must reach webhook-service (`payflow.webhook-dispatch.base-url`, default `http://localhost:8083`).

Start each app from `backend/` with Maven (examples):

```bash
./mvnw -pl payment-service spring-boot:run
./mvnw -pl webhook-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
```

No root-level Docker Compose stack for all services is defined yet; [infra/docker-compose.yml](infra/docker-compose.yml) is still a Phase 7 placeholder.

### With Docker

- **Tests**: use Docker so Testcontainers can run; command is still `./mvnw verify` under `backend/`.
- **Full stack**: when Phase 7 Compose (or your own compose file) defines Postgres, Kafka, and service images, map the same ports and environment variables implied by each `application.yml` (datasource URL, Kafka bootstrap, `payflow.webhook-dispatch.base-url` pointing at the webhook-service hostname inside the compose network, e.g. `http://webhook-service:8083`).

Until that file is populated, treat "with Docker" as **Testcontainers during CI/local verify**, plus any compose file you maintain yourself for Postgres + Kafka + apps.

---

## Configuration reference (Phase 4)

| Area | Location / keys |
|------|------------------|
| Payment outbox / Kafka topic | `payflow.outbox.topic` (default `payments.events`) |
| Webhook DLQ topic | `payflow.kafka.dlq-topic` (default `webhook.dlq`) |
| Internal dispatch toggle | `payflow.internal.dispatch-enabled` (webhook-service) |
| Notification → webhook HTTP | `payflow.webhook-dispatch.base-url`, `payflow.webhook-dispatch.enabled` |
| Dev API key → merchant | `payflow.security.api-keys` in payment-service and webhook-service |

---

## Related code

- Payment refunds: `backend/payment-service/`
- Webhooks: `backend/webhook-service/`
- Kafka consumer + dispatch client: `backend/notification-service/`
