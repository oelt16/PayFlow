# Merchant service

Spring Boot service for the **Merchants** bounded context: registration, DB-backed API key hashing (BCrypt), profile and lifecycle endpoints, and transactional outbox publication to Kafka topic `merchant.events`.

- **Port:** 8082 (see `application.yml`)
- **Schema:** PostgreSQL `merchants` (Flyway `V1__merchants_schema.sql`)
- **Run:** from `backend/`: `./mvnw -pl merchant-service spring-boot:run`

Payment and webhook services validate the same Bearer key against `merchants.merchants` (read-only); this service owns writes and key hashing for that table.
