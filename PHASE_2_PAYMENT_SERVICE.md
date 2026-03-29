# Phase 2 — payment service

This document describes what was implemented for **Phase 2** of [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) (payment bounded context: Spring Boot, persistence, REST, transactional outbox). It also covers how to **test** and **run** the service locally. Full multi-service **deployment** via Docker Compose is planned for **Phase 7**; this file includes a minimal runbook until then. In the spec document, Phase 2 is described in the implementation phases table and the REST, database, and testing sections.

## What was built

- **Spring Boot 3.3** application in [`backend/payment-service`](backend/payment-service) with entrypoint [`PaymentServiceApplication`](backend/payment-service/src/main/java/com/payflow/payment/PaymentServiceApplication.java).
- **Hexagonal layout**: `domain` (pure Java), `application` (use cases + ports), `infrastructure` (JPA, outbox), `api` (REST, DTOs, error handling).
- **PostgreSQL** schema `payments` managed by **Flyway** ([`V1__payments_schema.sql`](backend/payment-service/src/main/resources/db/migration/V1__payments_schema.sql)): `payments` table (including `client_secret`, `total_refunded`, card metadata, timestamps) and `outbox_events` for the transactional outbox.
- **REST API** under `/v1/payments`:
  - `POST /v1/payments` — create payment (amount in **minor units**, e.g. cents); stub card → last4 + brand only (no PAN stored).
  - `GET /v1/payments/{id}` and `GET /v1/payments` — read + paginated list with optional `status` filter.
  - `POST /v1/payments/{id}/capture` — idempotent capture (second call does not duplicate outbox events).
  - `POST /v1/payments/{id}/cancel` — cancel from `PENDING`.
- **Transactional outbox**: domain events drained after each write and inserted as `payment.created`, `payment.captured`, `payment.cancelled` (payload JSONB). Nothing publishes to Kafka yet (**Phase 3**).
- **API key stub**: `Authorization: Bearer <key>` validated against [`application.yml`](backend/payment-service/src/main/resources/application.yml) `payflow.security.api-keys` (replaced by merchant service in **Phase 5**).
- **Acquiring stub**: [`AcquiringPort`](backend/payment-service/src/main/java/com/payflow/payment/application/port/AcquiringPort.java) with a no-op adapter so capture stays a clear seam for a real acquirer later.

Default local settings: service **port 8081**, dev key `sk_test_dev` → merchant `mer_test_dev`.

## Prerequisites

- **Java 21**
- **Maven** (wrapper: `backend/mvnw`)
- **PostgreSQL 16** (or compatible) for running the app against a real database
- **Docker** (optional but recommended): required if you want **Testcontainers** integration tests to run instead of being skipped

## How to test

### All backend modules

From the repo root:

```bash
cd backend
./mvnw verify
```

### Payment service only

```bash
cd backend
./mvnw -pl payment-service verify
```

### What runs

| Layer | Tests |
| --- | --- |
| Domain | JUnit in `src/test/java/.../domain/` (no Spring) |
| Application / API | Mockito + [`@WebMvcTest`](backend/payment-service/src/test/java/com/payflow/payment/api/PaymentsControllerWebMvcTest.java) |
| Integration | [`PaymentApiIntegrationTest`](backend/payment-service/src/test/java/com/payflow/payment/integration/PaymentApiIntegrationTest.java) uses PostgreSQL via Testcontainers |

Integration tests use `@Testcontainers(disabledWithoutDocker = true)`. If **Docker is not available**, those tests are **skipped** and the build still passes. With **Docker running**, they execute against a disposable PostgreSQL container.

### JaCoCo

- **Domain** package [`com.payflow.payment.domain`](backend/payment-service/src/main/java/com/payflow/payment/domain): **100%** line and branch coverage is enforced on `mvn verify`.
- **Application + API**: separate coverage gate with lower thresholds (see [`payment-service/pom.xml`](backend/payment-service/pom.xml)).

## How to run locally (manual)

### 1. Start PostgreSQL

Create a database and user that match [`application.yml`](backend/payment-service/src/main/resources/application.yml) (or override with env / `SPRING_DATASOURCE_*`):

- URL: `jdbc:postgresql://localhost:5432/payflow`
- User / password: `payflow` / `payflow`

Example (adjust for your install):

```bash
createdb payflow
psql -d payflow -c "CREATE USER payflow WITH PASSWORD 'payflow'; GRANT ALL PRIVILEGES ON DATABASE payflow TO payflow;"
```

On first startup, **Flyway** creates the `payments` schema and tables.

### 2. Run the service

```bash
cd backend
./mvnw -pl payment-service spring-boot:run
```

Or build and run the jar:

```bash
cd backend
./mvnw -pl payment-service package -DskipTests
java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar
```

### 3. Call the API

Every `/v1/...` request needs:

```http
Authorization: Bearer sk_test_dev
```

Example create payment:

```bash
curl -sS -X POST http://localhost:8081/v1/payments \
  -H "Authorization: Bearer sk_test_dev" \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

`amount` is in **minor units** (e.g. `10000` = 100.00 USD). The response includes `clientSecret` on create; list/detail responses omit it.

## Deployment

- **Phase 7** (per spec) will add a full **Docker Compose** stack and Kubernetes manifests under [`infra/`](infra/). Today’s [`infra/docker-compose.yml`](infra/docker-compose.yml) is a placeholder only.
- Until then, you can deploy by:
  - Running the **jar** on a host or container with **PostgreSQL** reachable and env-based datasource configuration, or
  - Building a **container image** yourself (e.g. multi-stage Dockerfile: `mvn package` then `eclipse-temurin:21-jre` + copied jar) and pairing it with a managed PostgreSQL instance.

For production-style settings, override at least:

- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- `payflow.security.api-keys` (or replace this mechanism in Phase 5 with real merchant API keys)

## Further reading

- Full platform spec: [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) (REST API, database schema, implementation phases).
- Project conventions: [`.cursor/rules/payflow.mdc`](.cursor/rules/payflow.mdc).
