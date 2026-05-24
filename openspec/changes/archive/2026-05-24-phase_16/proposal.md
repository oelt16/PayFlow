# Proposal: Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

## Intent

Zero API docs across all services. Blocks onboarding, Phase 17 gateway, and external auditing. Add OpenAPI 3.1 + Swagger UI via annotations only — zero business logic changes.

## Scope

### In Scope
- Parent POM + 3 service POMs: springdoc dependency management
- 3 OpenAPI config beans with Bearer security scheme
- `@Tag`/`@Operation`/`@ApiResponse` on 5 controllers (17 endpoints)
- `@Schema` on ~20 DTOs (payment 9, merchant 7, webhook 6)
- Error response: extract inline `Map.of()` into annotated `ApiErrorResponse` records
- `@Hidden` on `/internal/webhooks/dispatch` + `/v1/internal/merchants/validate-key`
- `@Parameter` for `Idempotency-Key` on payment POST endpoints
- nginx.conf: `/swagger-ui/` + `/v3/api-docs` location blocks per service
- 3 `application.yml` — springdoc paths, server URL, Swagger UI path
- Single PR (~350-400 lines, mostly annotation boilerplate)

### Out of Scope
- notification-service (Kafka consumer, no REST API)
- Auth filter refactoring or path bypass changes
- CORS configuration changes
- Cross-service API aggregation / gateway (Phase 17)

## Capabilities

### New Capabilities
- `api-documentation`: OpenAPI 3.1 spec generation + Swagger UI across 3 REST services, with Bearer auth scheme, internal endpoint gating, and nginx routing

### Modified Capabilities
None

## Approach

Per-service springdoc-openapi (Approach 1 from exploration):

1. **Parent POM** — springdoc version property + dependency management entry
2. **3 service POMs** — add `springdoc-openapi-starter-webmvc-ui`
3. **3 `application.yml`** — `springdoc.paths-to-match`, `swagger-ui.path`, server URL override
4. **3 `OpenApiConfig` beans** — `@OpenAPIDefinition`, `SecurityScheme(bearer)`, `Info`, `Server`
5. **5 controllers** — `@Tag(class)`, `@Operation(method)`, `@ApiResponse(method)`
6. **~20 DTOs** — `@Schema(description, example)` on fields (class) or components (record)
7. **Error responses** — `ApiErrorResponse` record with `@Schema` per service or shared module
8. **Internal endpoints** — `@Hidden` on class or method
9. **nginx.conf** — proxy `/swagger-ui/` and `/v3/api-docs` to each backend service

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `backend/pom.xml` | Modified | springdoc dep mgmt + version property |
| `backend/*/pom.xml` (×3) | Modified | springdoc dependency |
| `backend/*/application.yml` (×3) | Modified | springdoc config |
| `backend/*/api/config/OpenApiConfig.java` (×3) | New | Config beans |
| `backend/*/api/*Controller.java` (×5) | Modified | Endpoint docs |
| `backend/*/api/dto/*.java` (~20) | Modified | Schema annotations |
| `backend/*/api/ApiExceptionHandler.java` (×3) | Modified | Error response schema |
| `frontend/nginx.conf` | Modified | Swagger UI routes |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `@Hidden` missed on internal endpoint | Low | Code review checklist; verify in Swagger after deploy |
| Auth filter blocks swagger paths | Low | `/v3/api-docs` ≠ `/v1/*` filter pattern — verify |
| Class vs record `@Schema` inconsistency | Med | Field-level on classes, component-level on records |
| `Map.of()` error responses lack schema | Med | Extract `ApiErrorResponse` record per service |

## Rollback Plan

`git revert` the merge commit — reverts all POM, config, and annotation changes atomically.

## Dependencies

- `springdoc-openapi-starter-webmvc-ui:2.6.0` (Spring Boot 3.3.6 compatible)

## Success Criteria

- [ ] Swagger UI accessible at `/swagger-ui.html` for all 3 REST services
- [ ] All 17 endpoints documented with request/response schemas
- [ ] Internal endpoints (`/internal/*`) hidden from public docs
- [ ] Bearer auth scheme shown in Swagger "Authorize" button
- [ ] Error responses show typed schema (not generic `object`)
- [ ] `Idempotency-Key` visible as header param on payment POST endpoints
