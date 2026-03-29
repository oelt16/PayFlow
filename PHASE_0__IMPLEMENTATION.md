# PayFlow: current implementation, tests, and runbook

This document describes what the repository contains today relative to [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt), how to run tests, how to run the **payment-service** locally, and what deployment means in this stage of the project.

## Spec phases vs this repo

| Spec phase | Scope (short) | Status in repo |
|------------|----------------|----------------|
| **Phase 1 — Domain core** | Payment aggregate, value objects, domain events, pure Java unit tests | **Done** in `backend/payment-service` under `com.payflow.payment.domain` |
| **Phase 2 — Payment service** | Spring Boot, JPA, REST (create, capture, cancel), outbox table, integration tests | **Largely done**; REST also includes **get** and **list** with pagination and optional `status` filter |
| **Phase 3 — Kafka** | Outbox relay, topics, consumers | **Not in payment-service yet** (no Kafka dependencies; outbox rows are written for a future relay) |
| **Phase 4+** | Refunds, webhooks, merchant service, frontend features, full Compose/K8s | **Not covered** by this document except as notes below |

Other backend modules (`merchant-service`, `webhook-service`, `notification-service`) are still placeholders.

## What the payment-service does today

- **Hexagonal layout:** `domain` → `application` → `infrastructure` / `api` (see [.cursor/rules/payflow.mdc](.cursor/rules/payflow.mdc)).
- **REST API** (base path `/v1/payments`): `POST` create, `GET /{id}`, `GET` list (`page`, `size`, optional `status`), `POST /{id}/capture`, `POST /{id}/cancel`.
- **Persistence:** PostgreSQL, schema `payments`, Flyway migration [V1__payments_schema.sql](backend/payment-service/src/main/resources/db/migration/V1__payments_schema.sql), JPA adapters.
- **Transactional outbox:** Domain events are appended to `outbox_events` in the same transaction as payment writes (spec section 6.2). Nothing publishes to Kafka yet.
- **Auth:** API keys from config ([application.yml](backend/payment-service/src/main/resources/application.yml)); requests use `Authorization: Bearer <key>` (see spec section 4.1).
- **Card handling:** Stubbed last4/brand/expiry; no real PAN storage (spec section 1.2).

Default dev key (from `application.yml`): `sk_test_dev` → merchant `mer_test_dev`.

## Prerequisites

- **Java 21** (see [.java-version](.java-version); use jenv or your JDK manager).
- **Docker Desktop** (or compatible Docker daemon) for **integration tests** that use Testcontainers.
- **PostgreSQL** if you run the app **locally** against the URLs in `application.yml` (not required only to run `mvn verify`, which uses Testcontainers for the full Spring test).

## How to test

From the backend root:

```bash
cd backend
./mvnw verify
```

This runs:

- **Unit tests** under `src/test/java/.../domain` (no Spring), plus other focused tests (e.g. `MoneyTest`, `PaymentTest`, `MerchantIdTest`, …).
- **`PaymentApplicationServiceTest`** (service layer with mocked ports).
- **`PaymentsControllerWebMvcTest`** (slice tests for the REST layer).
- **`PaymentApiIntegrationTest`** (`@SpringBootTest` + `MockMvc` + **Testcontainers PostgreSQL**). If Docker is not running, this class is skipped when `disabledWithoutDocker = true`.

**Coverage:** JaCoCo runs on `verify` for `payment-service`:

- **`com.payflow.payment.domain`:** line and branch coverage must be **100%** (build fails if not).
- **`application` + `api`:** minimum thresholds are enforced in [payment-service/pom.xml](backend/payment-service/pom.xml) (see `jacoco-check-service`).

**Run only payment-service tests:**

```bash
cd backend
./mvnw -pl payment-service verify
```

For a full `verify`, keep **Docker running** so integration tests execute.

## How to run the payment-service locally

1. Start **PostgreSQL 16** and create a database and user matching [application.yml](backend/payment-service/src/main/resources/application.yml):

   - URL pattern: `jdbc:postgresql://localhost:5432/payflow`
   - User / password: `payflow` / `payflow` (or override with env / local `application-local.yml`).

2. Ensure schema **`payments`** exists or let Flyway create it (`create-schemas: true` is set).

3. Run:

   ```bash
   cd backend/payment-service
   ../../mvnw spring-boot:run
   ```

   Or from `backend`:

   ```bash
   ./mvnw -pl payment-service spring-boot:run
   ```

4. **Port:** `8081` (see `server.port` in `application.yml`).

**Example (create payment):**

```bash
curl -sS -X POST http://localhost:8081/v1/payments \
  -H "Authorization: Bearer sk_test_dev" \
  -H "Content-Type: application/json" \
  -d '{"amount":10000,"currency":"USD","description":"Test","card":{"number":"4242424242424242","expMonth":12,"expYear":2027,"cvc":"123"},"metadata":{}}'
```

**Note:** [infra/docker-compose.yml](infra/docker-compose.yml) does not yet start PostgreSQL or the app (spec Phase 7). For a one-off DB, run Postgres via Docker manually, for example:

```bash
docker run --name payflow-pg -e POSTGRES_USER=payflow -e POSTGRES_PASSWORD=payflow -e POSTGRES_DB=payflow -p 5432:5432 -d postgres:16-alpine
```

## Deployment (what is possible now)

- **Runnable artifact:** Spring Boot repackages the service as an executable JAR when you build the module:

  ```bash
  cd backend
  ./mvnw -pl payment-service package -DskipTests
  ```

  The JAR is under `backend/payment-service/target/` (name like `payment-service-0.0.1-SNAPSHOT.jar`). Run with `java -jar ...` and the same datasource settings as production (env vars or external config).

- **Container / Kubernetes:** No `Dockerfile` or payment-service image is defined in this repo yet (spec Phase 7). CI in [.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml) builds and tests; image publish to GHCR is a later step.

- **Kafka / full stack:** Not part of deployment until Phase 3 and Compose/K8s are added.

## Related docs

- Root [README.md](README.md) for quick commands and repo layout.
- Full product and API specification: [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt).
