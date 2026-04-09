# Phase 5: Merchant service

This document describes what Phase 5 added to PayFlow, how the pieces connect, and how to run tests and local deployments with or without Docker.

For the full platform specification, see [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt).

---

## What shipped in Phase 5

### Merchant service

A full Spring Boot application under [backend/merchant-service/](backend/merchant-service/) with hexagonal layout (`domain`, `application`, `infrastructure`, `api`).

- **REST**
  - `POST /v1/merchants`: register a merchant (no `Authorization` header). Response includes a raw **`apiKey`** (`sk_test_` + 32 hex characters); this value is shown only here and at key rotation.
  - `GET /v1/merchants/me`: current merchant profile (`Authorization: Bearer <apiKey>`).
  - `DELETE /v1/merchants/me`: deactivate the merchant; the key stops working afterward.
  - `POST /v1/merchants/me/api-keys`: rotate the API key; response returns the new raw key once.
- **Persistence**: PostgreSQL schema **`merchants`**, tables `merchants.merchants` and `merchants.outbox_events` (Flyway [V1__merchants_schema.sql](backend/merchant-service/src/main/resources/db/migration/V1__merchants_schema.sql)).
- **API keys**: stored as **BCrypt hash** plus a **key prefix** (first 8 characters of the raw key) for indexed lookup before `BCrypt.matches` on the full token.
- **Domain events**: `MerchantCreatedEvent`, `MerchantDeactivatedEvent` appended to the transactional outbox and published to Kafka topic **`merchant.events`** by **OutboxRelay** (same pattern as payment-service). The notification-service does not consume this topic yet (per Phase 5 scope).
- **Errors**: JSON shape `{ "error": { "code", "message", "param?", "requestId" } }` on validation and domain failures; `401` for missing or invalid API key on protected routes.

Default HTTP port: **8082**. Main class: `com.payflow.merchant.MerchantServiceApplication`.

### How this relates to other services

**Payment-service** and **webhook-service** still authenticate using static **`payflow.security.api-keys`** in their own `application.yml` files. They do not call merchant-service to validate keys in this phase. To exercise payment or webhooks with a merchant created in merchant-service, you either keep using the dev keys already configured on those services or align `merchant-id` values manually when you introduce shared dev data.

---

## How to test

### Backend unit and integration tests (Maven)

From the repository root:

```bash
cd backend
./mvnw verify
```

- **Without Docker**: JVM-only tests run. Classes annotated with `@Testcontainers(disabledWithoutDocker = true)` are skipped when Docker is not available. You still run pure domain and application unit tests for merchant-service.
- **With Docker**: start Docker Desktop (or a compatible engine), then run the same command. Merchant-service integration tests start PostgreSQL and Kafka containers and exercise the REST API, BCrypt auth, and outbox publishing to `merchant.events`.

Target only merchant-service while iterating:

```bash
cd backend
./mvnw -pl merchant-service verify
```

Run a single test class:

```bash
./mvnw -pl merchant-service test -Dtest=MerchantApiIntegrationTest
```

### Manual API smoke checks (service already running)

Prerequisites: PostgreSQL with database and user matching [backend/merchant-service/src/main/resources/application.yml](backend/merchant-service/src/main/resources/application.yml) (default `jdbc:postgresql://localhost:5432/payflow`, user `payflow`, password `payflow`), Kafka at `localhost:9092`, and merchant-service started so Flyway can create the `merchants` schema.

**Register** (save the returned `apiKey`):

```bash
curl -sS -X POST "http://localhost:8082/v1/merchants" \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Shop","email":"demo@example.com"}'
```

**Profile** (replace `YOUR_API_KEY`):

```bash
curl -sS "http://localhost:8082/v1/merchants/me" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Rotate key**:

```bash
curl -sS -X POST "http://localhost:8082/v1/merchants/me/api-keys" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Deactivate**:

```bash
curl -sS -X DELETE "http://localhost:8082/v1/merchants/me" \
  -H "Authorization: Bearer YOUR_API_KEY"
```

To confirm outbox → Kafka locally, create a topic `merchant.events` (or rely on broker auto-create if enabled), register a merchant, wait for the scheduled relay (default poll every 500 ms), or trigger a flow that you know appends outbox rows and inspect the topic with your Kafka tooling.

---

## How to run and deploy locally

### Without Docker (processes on your machine)

1. **PostgreSQL 16**: create database and role (for example `payflow` / `payflow`). The service uses Flyway with `create-schemas: true` for schema **`merchants`**; you do not need to create tables by hand.
2. **Apache Kafka**: broker at `localhost:9092`, or override `spring.kafka.bootstrap-servers` (environment variable `SPRING_KAFKA_BOOTSTRAP_SERVERS` also works).
3. **Start merchant-service** from `backend/`:

```bash
./mvnw -pl merchant-service spring-boot:run
```

4. Optional overrides (examples):

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/payflow'
export SPRING_KAFKA_BOOTSTRAP_SERVERS='localhost:9092'
./mvnw -pl merchant-service spring-boot:run
```

There is no production Dockerfile or multi-service Compose file for merchant-service in this repository yet; [infra/docker-compose.yml](infra/docker-compose.yml) remains a Phase 7 placeholder.

### With Docker

- **Automated tests**: use Docker so Testcontainers can start Postgres and Kafka; run `./mvnw verify` (or `-pl merchant-service verify`) under `backend/`.
- **Full runtime stack**: until Phase 7 defines Compose (or you add your own), run Postgres and Kafka however you prefer (official images, cloud managed services, or a custom `docker compose` file). Map host ports **5432** and **9092** if you keep the default `application.yml`, or change URLs to match your containers.

If you add Compose later, typical patterns are:

- One Postgres container with database `payflow`, user/password matching the YAML.
- One Kafka (and ZooKeeper or KRaft) container exposing `9092` to the host.
- Merchant-service either run on the host against `localhost` ports, or as another service on the same Docker network using internal hostnames in `SPRING_DATASOURCE_URL` and `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

---

## Configuration reference (Phase 5)

| Area | Location / keys |
|------|------------------|
| HTTP port | `server.port` (default `8082`) |
| Datasource / Flyway schema | `spring.datasource.*`, `spring.flyway.schemas` / `default-schema` → `merchants` |
| Outbox / Kafka topic | `payflow.outbox.topic` (default `merchant.events`), `payflow.outbox.poll-interval-ms`, `payflow.outbox.batch-size`, `payflow.outbox.send-timeout-seconds` |
| Scheduling | Enabled when Spring profile is **not** `test` ([SchedulingConfiguration](backend/merchant-service/src/main/java/com/payflow/merchant/infrastructure/config/SchedulingConfiguration.java)); integration tests use `@ActiveProfiles("test")` so the scheduler does not run during `@SpringBootTest`. |

---

## Related code and docs

- Implementation: [backend/merchant-service/](backend/merchant-service/)
- Module README: [backend/merchant-service/README.md](backend/merchant-service/README.md)
- Domain and diagrams: [backend/merchant-service/docs/domain-model.md](backend/merchant-service/docs/domain-model.md), [backend/merchant-service/docs/class-diagram.md](backend/merchant-service/docs/class-diagram.md)
