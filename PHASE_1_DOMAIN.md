# Phase 1 — payments domain core

This document matches **Phase 1** in [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) (§11): the **payments bounded context as pure domain logic**; no requirement in that phase for a database, HTTP API, or Docker.

## What was implemented (Phase 1 scope)

Under `backend/payment-service/src/main/java/com/payflow/payment/domain/`:

- **Payment aggregate** — state machine: `PENDING` → capture / cancel / expire; `CAPTURED` → refunds (full / partial); domain events on each transition.
- **Value objects** — `Money`, `PaymentId`, `MerchantId`, `RefundId`, `CardDetails`, `CardBrand`, `PaymentStatus`.
- **Domain events** — `PaymentCreatedEvent`, `PaymentCapturedEvent`, `PaymentCancelledEvent`, `PaymentRefundedEvent`, `PaymentExpiredEvent` (payloads align with §5.3 for later Kafka mapping).
- **Domain exceptions** — e.g. invalid transitions, negative money, insufficient refundable amount.

**Hexagonal rule:** the `domain` package must not depend on Spring, JPA, or HTTP. Unit tests under `.../test/.../domain/` run without a Spring context.

The same Maven module (`payment-service`) also contains **later-phase** code (Spring Boot, REST, JPA, Flyway, outbox). That is outside strict Phase 1 but lives beside the domain for the same bounded context.

## How to run tests

**Prerequisites:** Java 21 (see [README.md](README.md)).

From the repo root:

```bash
cd backend
```

**All tests in `payment-service`** (domain unit tests plus any integration / API tests):

```bash
./mvnw -pl payment-service test
```

**Domain unit tests only** (fast, no Spring bootstrapping for those classes):

```bash
./mvnw -pl payment-service test -Dtest='com.payflow.payment.domain.**'
```

**Verify** (tests + JaCoCo reports and coverage checks configured in that module’s `pom.xml`):

```bash
./mvnw -pl payment-service verify
```

After `verify`, open the HTML coverage report:

- `backend/payment-service/target/site/jacoco/index.html`

JaCoCo is configured to enforce **100% line and branch coverage** on `com.payflow.payment.domain.**` (see `jacoco-check-domain` in [backend/payment-service/pom.xml](backend/payment-service/pom.xml)).

## Deployment (Phase 1 vs this repo)

**Per the spec, Phase 1 does not include deployment**; there is no Docker Compose stack required for Phase 1 alone.

**Running the full `payment-service` app locally** (needs PostgreSQL matching [application.yml](backend/payment-service/src/main/resources/application.yml)):

1. Start Postgres 16 with database `payflow`, user/password `payflow`, and ensure the server accepts connections on `localhost:5432`.
2. Flyway runs migrations from `src/main/resources/db/migration/` and creates the `payments` schema.
3. Start the app:

   ```bash
   cd backend
   ./mvnw -pl payment-service spring-boot:run
   ```

   Default port: **8081**. API key for local dev is configured under `payflow.security.api-keys` in `application.yml` (e.g. `Authorization: Bearer sk_test_dev`).

**Docker for the full local stack** (Postgres + Kafka + all services) is described in the spec as **Phase 7**. The file [infra/docker-compose.yml](infra/docker-compose.yml) is currently a placeholder until that phase is completed.

## See also

- [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) — §3 domain model, §5 events, §7.3 TDD scenarios, §11 phases.
- [README.md](README.md) — repo-wide prerequisites and `./mvnw verify` for the whole backend.
