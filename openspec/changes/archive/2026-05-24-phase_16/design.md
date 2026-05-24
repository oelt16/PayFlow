# Design: Phase 16 — OpenAPI 3.1 Documentation & Swagger UI

## Technical Approach

Per-service springdoc-openapi (Approach 1 from exploration). Each of the 3 REST services independently hosts its OpenAPI 3.1 spec at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. Nginx routes per-service Swagger UIs via separate location prefixes. Zero business logic changes — annotations only.

## Architecture Decisions

### Decision: Springdoc version management
| Option | Tradeoff | Decision |
|--------|----------|----------|
| Duplicate in each service POM | 3 copies to update, drift risk | ❌ |
| Parent POM property + dep mgmt entry | Single source of truth, matches existing pattern (bucket4j, junit) | ✅ |

### Decision: OpenAPI config bean approach
| Option | Tradeoff | Decision |
|--------|----------|----------|
| Shared library module | No shared module exists; overengineered for 3 config beans | ❌ |
| Per-service `OpenApiConfig` | Each service owns its Info/Security/Server; idiomatic Spring | ✅ |

### Decision: Internal endpoint gating
| Option | Tradeoff | Decision |
|--------|----------|----------|
| `@Hidden` on class | Zero-config, annotation-only; works for 2 internal controllers | ✅ |
| `springdoc.paths-to-match` | Excludes from ALL docs; per-service nuance; config drift risk | ❌ |
| `GroupedOpenApi` "Internal" group | Adds group config; overengineered for 2 endpoints | ❌ |

### Decision: Error response typing
| Option | Tradeoff | Decision |
|--------|----------|----------|
| Keep `ResponseEntity<Map<String, Object>>` | OpenAPI renders as generic `object` — no schema detail for consumers | ❌ |
| Per-service `ApiErrorResponse` record | Duplicates identical structure; but no shared module exists | ✅ |
| Create shared `lib/` Maven module | Overengineered; adds build complexity for 3 identical records | ❌ |

### Decision: nginx vs gateway for doc aggregation
| Option | Tradeoff | Decision |
|--------|----------|----------|
| springdoc gateway aggregator | Requires Phase 17 gateway; not built yet | ❌ |
| nginx location blocks per service | Direct mapping to current nginx pattern; each service gets own Swagger URL | ✅ |

### Decision: Swagger UI path strategy
| Option | Tradeoff | Decision |
|------|----------|---------|
| Same `/swagger-ui.html` per service (no prefix) | Would conflict if nginx serves all 3 services from root ; must namespace | ❌ |
| `/{service-name}/swagger-ui.html` via nginx | Clean URL per service; nginx rewrites to backend's default path | ✅ ✅ |

## Component Design

### OpenApiConfig (×3 — per service)
```java
@Configuration
@OpenAPIDefinition(info = @Info(title = "Payment Service API", version = "1.0",
    description = "Payment lifecycle — create, capture, cancel, refund"))
public class OpenApiConfig {
    @Bean
    public OpenAPI payFlowOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components().addSecuritySchemes("BearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                    .bearerFormat("API key")))
            .addServersItem(new Server().url("http://localhost:8081"));
    }
}
```
**Variations**: service name, description, port in Server URL (8081/8082/8083).

### Controller annotation pattern
- **Class**: `@Tag(name = "Payments", description = "...")`
- **Method**: `@Operation(summary = "...", description = "...")` + `@ApiResponse(responseCode = "201", description = "Created")`
- **Idempotency-Key**: `@Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "...")` on payment POST methods
- **Internal controllers**: `@Hidden` on class level (hides all methods)

### DTO annotation pattern
- **Classes** (e.g., `CreatePaymentRequest`): `@Schema(description = "...", example = "...")` on each field
- **Records** (e.g., `PaymentResponse`): `@Schema` annotation on each record component
- **Enums**: `@Schema(allowableValues = {"pending", "succeeded", "failed"})` where applicable

### ApiErrorResponse (per service)
```java
public record ApiErrorResponse(@Schema(description = "Error details") ErrorDetail error) {
    public record ErrorDetail(
        @Schema(example = "payment_not_found") String code,
        String message, String requestId, String param) {}
}
```
Each `ApiExceptionHandler` refactored to return `ResponseEntity<ApiErrorResponse>`.

### nginx location blocks (added to existing server block)
```nginx
location ~ ^/(payment|merchant|webhook)/swagger-ui {
    set $backend http://$1-service:808$<port_suffix>;
    proxy_pass $backend;
}
...
```

Wait — nginx variable interpolation in `$1-service` won't resolve correctly because the capture group is in the `set`. Use separate blocks for simplicity:

```ginx
location /payment/swagger-ui/ { proxy_pass http://payment-service:8081/swagger-ui/; }
location /payment/swagger-resources { proxy_pass http://payment-service:8081/swagger-resources; }
location /payment/v3/api-docs { proxy_pass http://payment-service:8081/v3/api-docs; }
```

## Data Flow

```
Browser → GET /payment/swagger-ui.html → nginx → http://payment-service:8081/swagger-ui.html
Swager UI JS → GET /payment/v3/api-docs → nginx → http://payment-service:8081/v3/api-docs
```

Same pattern for `/merchant/` and `/webhook/`.

## File Changes

**Dependencies (must be first)**:
| File | Action | Description |
|------|--------|-------------|
| `backend/pom.xml` | Modify | Add `springdoc-openapi.version=2.6.0` property + dep mgmt |
| `backend/payment-service/pom.xml` | Modify | Add springdoc dependency |
| `backend/merchant-service/pom.xml` | Modify | Add springdoc dependency |
| `backend/webhook-service/pom.xml` | Modify | Add springdoc dependency |

**Configuration (can be parallel with beans)**:
| File | Action | Description |
|------|--------|-------------|
| Each `.../src/main/resources/application.yml` (×3) | Modify | Add `springdoc.paths-to-match`, `springdoc.swagger-ui.path` |

**New files (config beans)**:
| File | Action | Description |
|------|--------|-------------|
| `payment-service/.../api/config/OpenApiConfig.java` | Create | Payment service OpenAPI def + security |
| `merchant-service/.../api/config/OpenApiConfig.java` | Create | Merchant service OpenAPI def + security |
| `webhook-service/.../api/config/OpenApiConfig.java` | Create | Webhook service OpenAPI def + security |

**Controllers (annotations only)**:
| File | Action | Description |
|------|--------|-------------|
| `PaymentsController.java` (payment) | Modify | `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter` (Idempotency-Key)|
| `MerchantsController.java` (merchant) | Modify | `@Tg`, `@Opertion`, `@ApiResponse` |
| `ValidateKeyController.java` (merchant/internal) | Modify | `@Hidden` class-level, + `@Operation` |
| `WebhooksController.java` (webhook) | Modify | `@Tg`, `@Opertion`, `@ApiResponse` |
| `InternalWebhookDispatchController.java` | Modify | `@Hidden` class-level |


**DTOs (~23 files)**:
|Files | Action | Description|
|------|--------|-------------|
| `.../.../.../.../.../.../.../.../.../.../.../... /.../... /.../... /... /... /... /... | Modify | Add `@Schema(description, example)` on all DTOs

**Error handling**:
| File | Action | Description |
|------|--------|-------------|
| Each `ApiExceptionHandler.java` (×3) | Modify | Extract `ApiErrorResponse` record; refactor `error(...)` + handlers to return `ResponseEntity<ApiErrorResponse>` |

**nginx**:
| File | Action | Description |
|------|--------|-------------|
| `frontend/nginx.conf` | Modify | Add 9 location blocks for 3 services × (`swagger-ui/`, `swagger-resources`, `v3/api-docs`) |

**Total**: ~31 files (1 parent POM, 3 service POMs, 3 app.yml, 3 config beans, 5 controllers, ~23 DTOs, 3 handlers, 1 nginx.conf)

## Dependency Graph

```
 Parent POM ──→ Service POMs ──→ application.yml
                                       ↓
                               OpenApiConfig beans ──→ Controller annotations + DTO @Schema
                                       │
                                       ↓
                              ApiErrorResponse refactor (handler return types)
                                       
  nginx.conf (independent — can parallel)
```

**Execution order**: POMs first (build won't compile without deps), then config + beans + annotations in any order, nginx last or parallel.

## Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Smoke | Each service starts and serves OpenAPI spec | `curl localhost:8081/v3/api-docs` after `mvn spring-boot:run` |
| UI | Each Swagger UI renders via nginx | Manual browse to `/{service}/swagger-ui.html` |
| Compliance | All 17 endpoints documented, internal hidden | Visual inspect rendered spec |

## Migration / Rollout

No data migration required. Deploy as single commit. To roll back: `git revert` the merge commit — all POM, config, and annotion changes revert atomically.

## Open Questions

- [ ] **Auth filter bypass**: `ApiKeyAuthenticationFilter` matches `/v1/*`. `swagger-ui/*` and `/v3/api-docs` do NOT start with `/v1/` — no bypass needed. Verify after deploy.
- [ ] **merchant-service ErrorResponse naming**: Existing `ErrorResponse` record (flat `{eror: String}`) is only used by ValidateKeyController. The new `ApiErrorResponse` has a different structure. Keep `ErrorResponse` for the internal controller; create new `ApiErrorResponse` for the exception handler. No naming conflict.
- [ ] **`CreatePaymentRequest.card` nesting**: `CardPayload` is a nested class-type DTO, not a record. `@Schema` on `card` field effectively documents the nested structure — verify in generated spec.