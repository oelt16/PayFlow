# Phase 3 — Kafka integration

This phase wires the **transactional outbox** in `payment-service` to **Apache Kafka** and adds a **notification-service** consumer that reads `payments.events` and logs each envelope. It matches the PayFlow spec: outbox relay, topic `payments.events`, partition key `merchantId`, envelope shape (`eventId`, `eventType`, `aggregateId`, `merchantId`, `occurredAt`, `payload`).

## What was implemented

| Area | Details |
|------|---------|
| **Outbox relay** | `OutboxRelay` polls unpublished rows, builds `PaymentEventEnvelope`, publishes JSON to `payments.events`, then marks rows published. Scheduled via `@EnableScheduling`; config under `payflow.outbox.*` in [`backend/payment-service/src/main/resources/application.yml`](backend/payment-service/src/main/resources/application.yml). |
| **Persistence** | `OutboxEventSpringDataRepository.findByPublishedFalseOrderByCreatedAtAsc(Pageable)` for ordered batches. |
| **Notification service** | Spring Boot app on port **8084**, `@KafkaListener` on `payments.events`, group `notification-service`, deserializes the same envelope and logs at INFO. See [`backend/notification-service/`](backend/notification-service/). |
| **Tests** | Unit: `OutboxRelayTest`, `PaymentEventConsumerTest`. Integration: `OutboxRelayIntegrationTest`, `PaymentApiIntegrationTest` (Kafka E2E), `NotificationConsumerIntegrationTest`; they use **Testcontainers** (PostgreSQL + official **`apache/kafka`** image, same default as `KafkaContainer`) when Docker is available. |

```text
HTTP → payment-service → DB + outbox row
              ↓
        OutboxRelay (scheduled)
              ↓
        Kafka: payments.events (key = merchantId)
              ↓
        notification-service (logs)
```

## How to test

### All modules (recommended)

From [`backend/`](backend/):

```bash
./mvnw -pl payment-service,notification-service -am verify
```

- **Unit and slice tests** run without Docker.
- **Integration tests** annotated with `@Testcontainers(disabledWithoutDocker = true)` **run only when Docker is available**. If Docker is not running (or no socket), those tests are **skipped**; the build still succeeds.

### Payment service only

```bash
./mvnw -pl payment-service test
```

### Notification service only

```bash
./mvnw -pl notification-service test
```

### Run a single integration class (Docker required)

```bash
./mvnw -pl payment-service test -Dtest=OutboxRelayIntegrationTest
./mvnw -pl notification-service test -Dtest=NotificationConsumerIntegrationTest
```

### CI note

Use a runner with **Docker** (for example `ubuntu-latest` in GitHub Actions) so Testcontainers can start Postgres and Kafka; otherwise integration coverage for this phase will be skipped.

### Where test output and reports go

- **Console:** Maven prints test progress, failures, and Spring Boot log lines (for integration tests) directly in the terminal when you run `./mvnw test` or `verify`.
- **Surefire reports (per module):** After a run, open `target/surefire-reports/` under the module that executed tests, for example:
  - [`backend/payment-service/target/surefire-reports/`](backend/payment-service/target/surefire-reports/) — `TEST-*.xml` per class (and any companion text reports Surefire emits).
  - [`backend/notification-service/target/surefire-reports/`](backend/notification-service/target/surefire-reports/) — same layout.
- **Save a full log to a file:**

  ```bash
  ./mvnw -pl payment-service,notification-service -am verify 2>&1 | tee kafka-phase-tests.log
  ```

- **More detail on failures:** add `-e` (stack traces) or `-X` (debug; very verbose):

  ```bash
  ./mvnw -pl payment-service test -e
  ```

- **IDE:** Running tests from the editor shows the same kind of output per test or class in the test runner panel.

## How to run locally (manual smoke)

Defaults in config assume:

- PostgreSQL: `localhost:5432`, database `payflow`, user/password `payflow` (schema `payments` created by Flyway).
- Kafka: `localhost:9092`.
- Payment API: port **8081**.
- Notification service: port **8084**.

Override with standard Spring properties (`spring.datasource.*`, `spring.kafka.bootstrap-servers`) or env vars (`SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, etc.).

1. Start Postgres and Kafka (see below, with or without Docker).
2. Start payment-service:

   ```bash
   ./mvnw -pl payment-service spring-boot:run
   ```

3. Start notification-service (separate terminal):

   ```bash
   ./mvnw -pl notification-service spring-boot:run
   ```

4. Create a payment (dev API key from `application.yml`):

   ```bash
   curl -sS -X POST http://localhost:8081/v1/payments \
     -H "Authorization: Bearer sk_test_dev" \
     -H "Content-Type: application/json" \
     -d '{"amount":10000,"currency":"USD","description":"Test","card":{"number":"4242424242424242","expMonth":12,"expYear":2027,"cvc":"123"},"metadata":{}}'
   ```

5. Within a few hundred milliseconds the relay should publish; check notification-service logs for a line like `Payment domain event: type=payment.created ...`.

## With Docker (infrastructure only)

The repo root does not yet ship a full Compose stack for all services ([`infra/docker-compose.yml`](infra/docker-compose.yml) is a placeholder until Phase 7). You can still run **dependencies** with Docker:

**PostgreSQL 16**

```bash
docker run --name payflow-postgres -e POSTGRES_DB=payflow -e POSTGRES_USER=payflow -e POSTGRES_PASSWORD=payflow \
  -p 5432:5432 -d postgres:16-alpine
```

**Kafka (official Apache image, KRaft)**

The integration tests use the same **`apache/kafka`** image Testcontainers expects (KRaft combined broker/controller). For a quick local broker on `9092`, use the image’s documented run (see [Apache Kafka on Docker Hub](https://hub.docker.com/r/apache/kafka)); a minimal pattern is:

```bash
docker run -d --name payflow-kafka -p 9092:9092 apache/kafka
```

If your tag requires extra environment variables for listeners or node id, follow the image README for that tag; the tests do not need ZooKeeper because the official image runs in KRaft mode.

Then run the two Spring Boot apps on the host as in the previous section (`localhost:9092` and `localhost:5432` match the defaults).

## Without Docker (local installs)

- Install **PostgreSQL 16**, create database `payflow` and user `payflow` / password `payflow` (or change `spring.datasource.*`).
- Install and start **Kafka 3.x** listening on `9092` (or point `spring.kafka.bootstrap-servers` at your broker).
- Run `./mvnw spring-boot:run` for each service as above.

Integration tests that depend on Testcontainers still need Docker; without it, use `./mvnw verify` and accept skipped integration tests, or run only tests that do not require containers.

## Deploying the JARs

There is no production Helm/Compose for these two services in this phase. Build artifacts:

```bash
./mvnw -pl payment-service,notification-service -am package -DskipTests
```

JARs:

- `backend/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar`
- `backend/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar`

Run:

```bash
java -jar backend/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar
java -jar backend/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar
```

Set the same datasource and Kafka bootstrap via environment variables or an external `application.yml` / profile for the target environment. Ensure the **`payments.events`** topic exists (or enable auto-create on the broker for dev). Notification service must reach the **same** cluster and topic name as payment-service’s producer.

## Related files

- Payment relay and envelope: [`backend/payment-service/src/main/java/com/payflow/payment/infrastructure/kafka/`](backend/payment-service/src/main/java/com/payflow/payment/infrastructure/kafka/)
- Notification service layout (diagrams): [`backend/notification-service/docs/`](backend/notification-service/docs/)
- Shared integration test wiring: [`backend/payment-service/src/test/java/com/payflow/payment/integration/PaymentIntegrationInfrastructure.java`](backend/payment-service/src/test/java/com/payflow/payment/integration/PaymentIntegrationInfrastructure.java)
- Spec reference: [`PayFlow_Specification.docx.txt`](PayFlow_Specification.docx.txt) (sections 5 — Kafka, 6 — outbox)
