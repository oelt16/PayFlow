# Merchant service

Spring Boot service for the **Merchants** bounded context: registration, DB-backed API key hashing (BCrypt), profile and lifecycle endpoints, and transactional outbox publication to Kafka topic `merchant.events`.

- **Port:** 8082 (see `application.yml`)
- **Schema:** PostgreSQL `merchants` (Flyway `V1__merchants_schema.sql`)
- **Run:** from `backend/`: `./mvnw -pl merchant-service spring-boot:run`

Payment and webhook services continue to use static `payflow.security.api-keys` in their own configs; this service is the source of truth for merchant records and key hashing.
