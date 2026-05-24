# Tasks: Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~380-450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (annotation-only, additive, low risk) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

**Rationale**: ~31 files but changes are additive boilerplate — POM entries, config blocks, annotations. No business logic. Chained PRs would split coherent work and add overhead. Single PR with `size:exception` if over 400 lines.

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All changes (POM → config → beans → controllers → DTOs → nginx) | PR 1 | Single PR to main; all changes are additive annotations |

## Dependency Graph

```
Parent POM (T1)
  ├── Service POMs (T2) ──→ app.yml (T3) ──→ OpenApiConfig (T4)
  │                                              │
  │                              ┌────────────────┤
  │                              ↓                ↓
  │                      Controllers (T6)     DTOs (T7)
  │                              │
  │                              ↓
  │                      nginx.conf (T8)
  │
  └── ApiExceptionHandler (T5) ←─── (parallel with T3-T4; depends on POM)
```

## Phase 1: Build & Dependency Setup

- [x] **1.1 Parent POM** — Add `<springdoc-openapi.version>2.6.0</springdoc-openapi.version>` property + `<dependencyManagement>` entry for `springdoc-openapi-starter-webmvc-ui` [R10]. File: `backend/pom.xml`. ~5 lines.
- [x] **1.2 Service POMs (×3)** — Add `springdoc-openapi-starter-webmvc-ui` dependency to payment/merchant/webhook service POMs [R10]. Files: `backend/payment-service/pom.xml`, `backend/merchant-service/pom.xml`, `backend/webhook-service/pom.xml`. ~15 lines (5 each). Depends on: 1.1.
- [x] **1.3 Verify** — `mvn dependency:resolve` resolves springdoc:2.6.0 for all 3 services; notification-service unaffected [R10, R1].

## Phase 2: Configuration

- [x] **2.1 application.yml (×3)** — Add `springdoc.paths-to-match: /v1/**`, `springdoc.swagger-ui.path: /swagger-ui.html`, `springdoc.swagger-ui.operations-sorter: method`, `springdoc.swagger-ui.tags-sorter: alpha`, `springdoc.swagger-ui.display-request-duration: true` to payment/merchant/webhook [R2]. Files: 3 `application.yml`. ~18 lines. Depends on: 1.2.
- [x] **2.2 OpenApiConfig.java (×3)** — Create per-service `@Configuration` bean with `@OpenAPIDefinition`, Bearer SecurityScheme, `Info`, `Server` URL (8081/8082/8083) [R5]. New files: `backend/*/.../api/config/OpenApiConfig.java`. ~120 lines. Depends on: 1.2.

## Phase 3: Typed Error Response

- [x] **3.1 Payment ApiExceptionHandler** — Add `ApiErrorResponse` record (ErrorDetail with code/message/requestId/param + `@Schema`); refactor all handlers + `error()` to return `ResponseEntity<ApiErrorResponse>` [R7]. File: `backend/payment-service/.../api/ApiExceptionHandler.java`. ~40 lines. Depends on: 1.2.
- [x] **3.2 Merchant ApiExceptionHandler** — Same pattern. Keep existing `ErrorResponse` record for ValidateKeyController (separate concern) [R7]. File: `backend/merchant-service/.../api/ApiExceptionHandler.java`. ~30 lines. Depends on: 1.2.
- [x] **3.3 Webhook ApiExceptionHandler** — Same pattern as payment [R7]. File: `backend/webhook-service/.../api/ApiExceptionHandler.java`. ~30 lines. Depends on: 1.2.

## Phase 4: Controller Annotations

- [x] **4.1 PaymentsController** — `@Tag(name = "Payments")` on class; `@Operation` + `@ApiResponse` on 7 endpoints; `@Parameter(in = HEADER)` Idempotency-Key on `POST /v1/payments` + `POST /{id}/refunds` [R3, R8]. File: `backend/payment-service/.../api/PaymentsController.java`. ~35 lines. Depends on: 1.2.
- [x] **4.2 MerchantsController** — `@Tag(name = "Merchants")`; `@Operation` + `@ApiResponse` on 4 endpoints. No `@SecurityRequirement` on `POST /v1/merchants` (public) [R3, R5]. File: `backend/merchant-service/.../api/MerchantsController.java`. ~20 lines. Depends on: 1.2.
- [x] **4.3 ValidateKeyController** — `@Hidden` class-level; hide from public docs. All methods inherit `@Hidden` [R6]. File: `backend/merchant-service/.../api/internal/ValidateKeyController.java`. ~3 lines. Depends on: 1.2.
- [x] **4.4 WebhooksController** — `@Tag(name = "Webhooks")`; `@Operation` + `@ApiResponse` on 4 endpoints [R3]. File: `backend/webhook-service/.../api/WebhooksController.java`. ~20 lines. Depends on: 1.2.
- [x] **4.5 InternalWebhookDispatchController** — `@Hidden` class-level (internal endpoint) [R6]. File: `backend/webhook-service/.../api/InternalWebhookDispatchController.java`. ~2 lines. Depends on: 1.2.

## Phase 5: DTO Schema Annotations

- [x] **5.1 Payment DTOs (×9)** — Add `@Schema(description, example)` on fields (classes: CreatePaymentRequest, CardPayload, CancelPaymentRequest, CreateRefundRequest) or components (records: PaymentResponse, PaymentListResponse, RefundResponse, RefundListResponse, CardResponse) [R4]. ~35 lines. Depends on: 1.2.
- [x] **5.2 Merchant DTOs (×7)** — Same pattern on all DTOs (RegisterMerchantRequest class; records: MerchantResponse, RegisterMerchantResponse, RotateApiKeyResponse, ErrorResponse, ValidateKeyRequest, ValidateKeyResponse) [R4]. ~25 lines. Depends on: 1.2.
- [x] **5.3 Webhook DTOs (×7)** — Same pattern (RegisterWebhookRequest, DispatchRequest classes; records: WebhookRegisteredResponse, WebhookSummaryResponse, WebhookListResponse, DeliveryResponse, DeliveryListResponse) [R4]. ~25 lines. Depends on: 1.2.

## Phase 6: nginx Routing

- [x] **6.1 nginx.conf** — Add 9 location blocks (3 services × 3 paths): `/{service}/swagger-ui/`, `/{service}/swagger-resources`, `/{service}/v3/api-docs` proxied to each backend [R9]. File: `frontend/nginx.conf`. ~30 lines. Independent — can be done in parallel with any phase.

## End-to-End Verification

| Check | Command / Action | Expected |
|-------|-----------------|----------|
| R1 | `curl localhost:8081/v3/api-docs` | Valid OpenAPI 3.1 JSON |
| R2 | Browse `http://localhost:8081/swagger-ui.html` | Swagger UI loads |
| R3 | Inspect `POST /v1/payments` in Swagger | Shows 201/400/401 responses |
| R4 | Inspect `CreatePaymentRequest` schema | `amount` has description + example |
| R5 | Click "Authorize" in Swagger | Bearer token input appears |
| R6 | Search for `/internal/webhooks/dispatch` in webhook spec | Not found |
| R7 | Inspect `ApiErrorResponse` schema | 4 typed string fields |
| R8 | Expand `POST /v1/payments` | Idempotency-Key shown as header |
| R9 | `GET /webhook/v3/api-docs` via nginx | Proxied to webhook-service:8083 |
| R10 | `mvn dependency:resolve` | springdoc resolved to 2.6.0 |
