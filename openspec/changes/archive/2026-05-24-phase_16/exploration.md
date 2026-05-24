## Exploration: OpenAPI 3.1 Documentation & Swagger UI

### Current State

The PayFlow backend has **zero** API documentation infrastructure today. There are no springdoc-openapi dependencies, no OpenAPI/Swagger configuration beans, and no controller or DTO annotations for documentation generation.

The system consists of **3 RESTful services** (payment-service, merchant-service, webhook-service) and **1 Kafka consumer** (notification-service) that exposes no REST endpoints. The frontend nginx reverse-proxies requests to the three REST services.

### Affected Areas

#### Controllers (5 total, across 3 services)

**payment-service** (port 8081) — 1 controller, 7 endpoints:

| Method | Path | Handler | Auth | Request Body | Response |
|--------|------|---------|------|-------------|----------|
| POST | `/v1/payments` | `create()` | Bearer | `CreatePaymentRequest` | `PaymentResponse` (201) |
| GET | `/v1/payments` | `list()` | Bearer | query: page, size, status | `PaymentListResponse` |
| GET | `/v1/payments/{id}` | `get()` | Bearer | — | `PaymentResponse` |
| POST | `/v1/payments/{id}/capture` | `capture()` | Bearer | — | `PaymentResponse` |
| POST | `/v1/payments/{id}/cancel` | `cancel()` | Bearer | `CancelPaymentRequest` | `PaymentResponse` |
| POST | `/v1/payments/{id}/refunds` | `createRefund()` | Bearer | `CreateRefundRequest` | `RefundResponse` (201) |
| GET | `/v1/payments/{id}/refunds` | `listRefunds()` | Bearer | — | `RefundListResponse` |

- File: `backend/payment-service/src/main/java/com/payflow/payment/api/PaymentsController.java`
- Has NO `@Tag`, `@Operation`, or `@ApiResponse` annotations.

**merchant-service** (port 8082) — 2 controllers, 5 endpoints:

| Method | Path | Handler | Auth | Request Body | Response |
|--------|------|---------|------|-------------|----------|
| POST | `/v1/merchants` | `register()` | **PUBLIC** | `RegisterMerchantRequest` | `RegisterMerchantResponse` (201) |
| GET | `/v1/merchants/me` | `me()` | Bearer | — | `MerchantResponse` |
| DELETE | `/v1/merchants/me` | `deactivateMe()` | Bearer | — | `204 No Content` |
| POST | `/v1/merchants/me/api-keys` | `rotateApiKey()` | Bearer | — | `RotateApiKeyResponse` |
| POST | `/v1/internal/merchants/validate-key` | `validateKey()` | Internal* | `ValidateKeyRequest` | `ValidateKeyResponse` \| `ErrorResponse` |

> \* The `validate-key` endpoint at `/v1/internal/merchants/validate-key` goes through the auth filter (starts with `/v1/`) but is an **internal cross-service API** used by payment/webhook services for API key cache validation. It should be documented with restricted visibility.

- Files: `.../merchant/api/MerchantsController.java`, `.../merchant/api/internal/ValidateKeyController.java`
- Has NO OpenAPI annotations.

**webhook-service** (port 8083) — 2 controllers, 5 endpoints:

| Method | Path | Handler | Auth | Request Body | Response |
|--------|------|---------|------|-------------|----------|
| POST | `/v1/webhooks` | `register()` | Bearer | `RegisterWebhookRequest` | `WebhookRegisteredResponse` (201) |
| GET | `/v1/webhooks` | `list()` | Bearer | — | `WebhookListResponse` |
| DELETE | `/v1/webhooks/{id}` | `deactivate()` | Bearer | — | `204 No Content` |
| GET | `/v1/webhooks/{id}/deliveries` | `deliveries()` | Bearer | — | `DeliveryListResponse` |
| POST | `/internal/webhooks/dispatch` | `dispatch()` | **NONE** | `DispatchRequest` | `204 No Content` |

> ⚠️ **Critical**: `InternalWebhookDispatchController` is mapped at `/internal/webhooks` (no `/v1/` prefix). It **bypasses** `ApiKeyAuthenticationFilter` entirely. The only protection is a feature flag (`dispatch-enabled`). This endpoint is called **internally** by notification-service. It should be excluded from public OpenAPI docs.

- Files: `.../webhook/api/WebhooksController.java`, `.../webhook/api/InternalWebhookDispatchController.java`
- Has NO OpenAPI annotations.

**notification-service** (port 8084) — **0 controllers, no REST API**
- Uses `spring-boot-starter` (NOT `spring-boot-starter-web`)
- Only dependency is `spring-web` (for `RestTemplate` to call webhook-service)
- **No `@RestController`, `@RequestMapping`, or mapping annotations exist**
- Exposes ONLY Actuator endpoints (health, info, prometheus, metrics)
- **Conclusion**: No OpenAPI docs needed for notification-service

#### DTOs Requiring @Schema Annotations (~20 total)

**payment-service** (9 DTOs):
- `CreatePaymentRequest` (class) — fields: amount, currency, description, card, metadata
- `CardPayload` (class) — fields: number, expMonth, expYear, cvc
- `CancelPaymentRequest` (class) — fields: reason
- `CreateRefundRequest` (class) — fields: amount, currency, reason
- `PaymentResponse` (record) — 13 fields (id, amount, currency, status, description, clientSecret, metadata, card, createdAt, expiresAt, capturedAt, cancelledAt, totalRefunded, amountRefunded)
- `CardResponse` (record) — 4 fields (last4, brand, expMonth, expYear)
- `RefundResponse` (record) — 6 fields (id, paymentId, amount, currency, reason, createdAt)
- `PaymentListResponse` (record) — 4 fields (content, totalElements, page, size)
- `RefundListResponse` (record) — 2 fields (data, totalElements)

**merchant-service** (7 DTOs):
- `RegisterMerchantRequest` (class) — fields: name, email
- `RegisterMerchantResponse` (record) — 5 fields (id, name, email, apiKey, createdAt)
- `MerchantResponse` (record) — 5 fields (id, name, email, active, createdAt)
- `RotateApiKeyResponse` (record) — 1 field (apiKey)
- `ErrorResponse` (record) — 1 field (error)
- `ValidateKeyRequest` (record) — 1 field (keyPrefix)
- `ValidateKeyResponse` (record) — 3 fields (merchantId, keyHash, isActive)

**webhook-service** (6 DTOs):
- `RegisterWebhookRequest` (class) — fields: url, events
- `WebhookRegisteredResponse` (record) — 5 fields (id, url, events, secret, createdAt)
- `WebhookSummaryResponse` (record) — 5 fields (id, url, events, active, createdAt)
- `WebhookListResponse` (record) — 1 field (content)
- `DeliveryResponse` (record) — 8 fields (id, eventType, status, attempts, lastAttemptAt, nextRetryAt, lastError, createdAt)
- `DeliveryListResponse` (record) — 2 fields (data, totalElements)
- `DispatchRequest` (class) — fields: merchantId, eventType, eventPayload (JsonNode)

#### Error Response Schema (shared pattern)

All 3 services use an identical error response format:

```json
{
  "error": {
    "code": "payment_not_found",
    "message": "Payment not found: ...",
    "requestId": "req-abc123",
    "param": "id"
  }
}
```

This is not defined as a reusable class — each `ApiExceptionHandler` builds it inline via `Map.of()`. A shared `ErrorResponse` model should be created or @Schema on the handler return type used instead.

#### Current nginx.conf

File: `frontend/nginx.conf`

Routes:
- `/api/v1/payments/*` → `http://payment-service:8081` (strips `/api` prefix)
- `/api/v1/merchants/*` → `http://merchant-service:8082` (strips `/api` prefix)
- `/api/v1/webhooks/*` → `http://webhook-service:8083` (strips `/api` prefix)
- `/` → static files (`index.html`)

**No Swagger UI or OpenAPI doc routes exist yet.** They must be added.

#### Current Dependencies

**No service has springdoc-openapi.** The spec v3 document references it as planned, but it was never implemented.

Parent POM (`backend/pom.xml`):
- No springdoc dependency management or version property
- Java 21, Spring Boot 3.3.6

#### Auth Mechanism

All 3 REST services use a component-scanned `ApiKeyAuthenticationFilter extends OncePerRequestFilter`:

- **Header**: `Authorization: Bearer <api_key>`
- **Behavior**:
  - Payment-service: `/v1/*` requires valid Bearer token
  - Merchant-service: `/v1/*` requires auth **except** `POST /v1/merchants` (public registration)
  - Webhook-service: `/v1/*` requires valid Bearer token; `/internal/*` bypasses filter entirely
- **Error format** (unauthorized): 401 with `{"error": {"code": "invalid_api_key", "message": "...", "requestId": "..."}}`
- No Spring Security `SecurityFilterChain` is configured — auth is purely filter-based

#### Existing OpenAPI/Swagger Config

**None found.** Zero references to `springdoc`, `swagger`, or `OpenAPI` in any Java source, POM, or config file.

#### Project Structure (relevant paths)

```
backend/
├── payment-service/src/main/java/com/payflow/payment/api/
│   ├── PaymentsController.java         ← annotate
│   ├── PaymentApiMapper.java
│   ├── RefundApiMapper.java
│   ├── ApiExceptionHandler.java        ← annotate
│   ├── dto/                            ← annotate all
│   └── security/ApiKeyAuthenticationFilter.java
├── merchant-service/src/main/java/com/payflow/merchant/api/
│   ├── MerchantsController.java        ← annotate
│   ├── MerchantApiMapper.java
│   ├── ApiExceptionHandler.java        ← annotate
│   ├── dto/                            ← annotate all
│   ├── internal/ValidateKeyController.java ← annotate (as internal)
│   └── security/ApiKeyAuthenticationFilter.java
├── webhook-service/src/main/java/com/payflow/webhook/api/
│   ├── WebhooksController.java         ← annotate
│   ├── InternalWebhookDispatchController.java ← EXCLUDE or mark internal
│   ├── WebhookApiMapper.java
│   ├── ApiExceptionHandler.java        ← annotate
│   ├── dto/                            ← annotate all
│   └── security/ApiKeyAuthenticationFilter.java
└── notification-service/src/main/java/...
    └── (NO controllers — skip entirely)
frontend/
└── nginx.conf                          ← add Swagger routes
```

### Approaches

#### 1. Per-Service springdoc-openapi (Recommended)
Add `springdoc-openapi-starter-webmvc-ui` to each of the 3 REST services. Each service independently hosts its own OpenAPI spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. Nginx aggregates the UIs via separate location blocks.

- **Pros**: Independent deployability, each service owns its docs, standard approach, zero cross-service coupling
- **Cons**: 3 separate POM changes, 3 separate configuration blocks, user must know which service they want docs for
- **Effort**: Medium (3 POM files, 3 config blocks, but boilerplate is identical)

#### 2. Aggregated OpenAPI via nginx + Single Entry Point
Same as #1 but creates a single OpenAPI definition aggregating all 3 specs, possibly via a simple API gateway component or using springdoc's GroupedOpenApi to cross-reference.

- **Pros**: Single Swagger UI for all APIs, cleaner user experience
- **Cons**: Over-engineered for 3 services, requires aggregation component or complex nginx setup, CORS considerations
- **Effort**: High (requires aggregation component or nginx config with spec merging)

#### 3. Minimal — Only Add springdoc, No DTO Annotations
Add springdoc dependency and basic config, relying on auto-detection without `@Schema` annotations. Accept that DTO fields appear with auto-detected names and no descriptions.

- **Pros**: Fastest to implement, minimal code changes
- **Cons**: Poor developer experience, missing descriptions, no example values, no schema constraints visible
- **Effort**: Low (but produces low-quality docs)

### Recommendation

**Approach #1 (Per-Service)** is the correct fit for this microservices architecture. Here's the implementation plan:

1. **Parent POM** (`backend/pom.xml`): Add springdoc-openapi version property and dependency management entry
2. **3 service POMs**: Add `springdoc-openapi-starter-webmvc-ui` dependency
3. **3 application.yml**: Add springdoc config (paths, swagger-ui path, server URL per service)
4. **3 OpenAPI config beans**: Create `@Configuration` classes with `@OpenAPIDefinition`, security scheme (Bearer JWT-style), and server info
5. **5 controllers**: Add `@Tag` at class level, `@Operation` + `@ApiResponse` at method level
6. **~20 DTOs**: Add `@Schema` annotations (description, example, allowable values)
7. **3 ApiExceptionHandlers**: Annotate return types or create a shared error response class with `@Schema`
8. **nginx.conf**: Add location blocks for `/swagger-ui/`, `/v3/api-docs`, and (optionally) `/swagger-resources`
9. **Internal endpoints**: Mark with `@Hidden` or group separately as "Internal API" via springdoc grouping

### Risks

1. **Internal endpoint exposure**: `POST /internal/webhooks/dispatch` and `POST /v1/internal/merchants/validate-key` must be **excluded** or hidden from public docs. springdoc's `@Hidden` annotation or path-based grouping should be used.

2. **Auth filter vs Swagger paths**: The `ApiKeyAuthenticationFilter` blocks all `/v1/*` paths. Swagger UI resources at `/v3/api-docs` and `/swagger-ui/*` do NOT start with `/v1/` so they will NOT be caught by the filter. However, if using `GroupedOpenApi` with paths, ensure the swagger paths themselves are in the filter's bypass list.

3. **notification-service**: It has NO REST API (pure Kafka consumer). Do NOT add springdoc to it. Only Actuator endpoints exist.

4. **Error response format**: Currently built inline via `Map.of()` in `ApiExceptionHandler`. To get proper OpenAPI schema for error responses, you should either: (a) Create a `record ErrorResponse` in each service with `@Schema`, or (b) Use a shared library approach. The current inline maps won't generate in OpenAPI output.

5. **DTO records vs classes**: Payment/merchant/webhook DTOs are mixed between Java `class` (with getters/setters) and Java `record`. springdoc handles both, but `@Schema` annotation placement differs slightly (on record components vs fields). The `register` endpoint's `RegisterMerchantRequest` and `CreatePaymentRequest` use classes, while most responses use records.

6. **IdempotencyFilter**: Payment-service has an `IdempotencyFilter` that reads `Idempotency-Key` header. This should be documented as a header parameter on relevant endpoints (POST /v1/payments, POST /v1/payments/{id}/refunds).

7. **CORS**: If the frontend needs to access Swagger UI from a different origin or if Swagger UI is used from outside the nginx proxy, CORS configuration may be needed.

### Ready for Proposal

**Yes** — exploration is complete. All controllers, DTOs, auth mechanisms, and configuration files have been identified. The orchestrator should tell the user:

> Exploration for Phase 16 is complete. 5 controllers across 3 services (payment, merchant, webhook) with 20 DTOs requiring `@Schema` annotations. notification-service has NO REST controllers and should be excluded. No existing springdoc dependencies found. Recommendation: per-service springdoc-openasi setup with `@Tag`/`@Operation`/`@ApiResponse`/`@Schema` annotations. Internal endpoints must be hidden from public docs. Ready to proceed to sdd-propose.
