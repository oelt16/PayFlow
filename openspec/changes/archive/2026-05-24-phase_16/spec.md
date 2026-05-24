# api-documentation Specification

## Purpose

Add OpenAPI 3.1 documentation + Swagger UI to 3 REST services via springdoc-openapi annotations. Enables interactive API exploration for onboarding, interview demos, and external auditing. Prerequisite for Phase 17 API gateway aggregation. Zero business logic changes — documentation-only.

## Requirements

### R1: OpenAPI Spec Endpoint

Each REST service MUST expose a valid OpenAPI 3.1 document at `/v3/api-docs`.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | service is running | GET `/v3/api-docs` | returns 200 with valid OpenAPI 3.1 JSON |
| Not found | notification-service (Kafka consumer) | GET `/v3/api-docs` | returns 404 (no springdoc dependency) |

### R2: Swagger UI

Each REST service MUST serve Swagger UI at `/swagger-ui.html`.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | payment-service running | open `/swagger-ui.html` on port 8081 | Swagger UI loads with payment endpoints |
| Invalid service | notification-service | open `/swagger-ui.html` on port 8084 | returns 404 |

### R3: Public Endpoint Documentation

All 17 public endpoints MUST have `@Tag` (class), `@Operation` (method), and `@ApiResponse` annotations.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | payment-service running | inspect `POST /v1/payments` in Swagger UI | shows summary, 201/400/401 responses |
| Edge case | `POST /v1/merchants` (public, no auth) | inspect in Swagger UI | shows 201 but not 401 (no auth required) |

### R4: DTO Schema Annotations

All ~20 DTOs MUST have `@Schema` with `description`, `example`, and constraint attributes.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | CreatePaymentRequest | inspect in OpenAPI spec | `amount` shows description + example + minimum |
| Edge case | Record DTO (e.g., PaymentResponse) | inspect `@Schema` on component | annotations present on record-level or field-level |

### R5: Bearer Auth Scheme

Each OpenAPI config MUST declare a Bearer JWT security scheme visible in Swagger UI's "Authorize" button.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | payment-service Swagger UI | click "Authorize" button | Bearer token input shown |
| Edge case | `POST /v1/merchants` (public) | inspect endpoint in Swagger | lock icon absent (no security requirement) |

### R6: Internal Endpoints Hidden

`POST /internal/webhooks/dispatch` and `POST /v1/internal/merchants/validate-key` MUST be annotated with `@Hidden` and NOT appear in any public OpenAPI document.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | webhook-service OpenAPI spec | search for `/internal/webhooks/dispatch` | endpoint NOT present |
| Edge case | merchant-service OpenAPI spec | search for `/v1/internal/merchants/validate-key` | endpoint NOT present |

### R7: Typed Error Response Schema

Each service MUST expose `ApiErrorResponse` (or equivalent) as a `@Schema`-annotated type showing `code`, `message`, `requestId`, `param` fields — NOT a generic `object`.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | payment-service OpenAPI spec | inspect error response schema | shows 4 typed string fields |
| Edge case | merchant-service has no `param` field | inspect error schema for validate-key | `param` is optional (nullable or absent) |

### R8: Idempotency-Key Header

`Idempotency-Key` header MUST appear as a `@Parameter` on `POST /v1/payments` and `POST /v1/payments/{id}/refunds`.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | payment-service Swagger UI | expand `POST /v1/payments` | Idempotency-Key shown as required header |
| Edge case | `POST /v1/payments/{id}/refunds` | expand in Swagger UI | Idempotency-Key shown as optional header |

### R9: nginx Proxy Routing

nginx MUST proxy `/swagger-ui/` and `/v3/api-docs` to all 3 REST services with distinct upstream paths.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | all services running | `GET /swagger-ui/payment/` | nginx proxies to payment-service:8081 |
| Edge case | one service down | `GET /swagger-ui/merchant/` | nginx returns 502 for that service, others unaffected |

### R10: Version Management

springdoc-openapi dependency version MUST be defined as a Maven property in the parent POM with a `<dependencyManagement>` entry, consumed by all 3 service POMs.

| Scenario | GIVEN | WHEN | THEN |
|----------|-------|------|------|
| Happy path | parent `pom.xml` | `mvn dependency:resolve` | springdoc resolved to property-defined version |
| Edge case | version property missing | build | compilation fails with missing dependency |

## Non-functional Constraints

- Must NOT break any existing endpoint behavior (documentation-only change)
- Must NOT leak internal endpoints to public OpenAPI docs
- Springdoc dependency MUST NOT be added to notification-service
- All services MUST continue to build and pass tests with `mvn verify`
- Must work behind nginx reverse proxy without CORS changes
- Bearer auth filter (`ApiKeyAuthenticationFilter`) MUST NOT block `/v3/api-docs` or `/swagger-ui/*` paths

## Out of Scope

- notification-service documentation (no REST API)
- CORS configuration changes
- Auth filter refactoring or path bypass changes
- Cross-service API aggregation / gateway (Phase 17)
- Custom Swagger UI theming or branding
