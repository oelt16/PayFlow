# Design: Fix Pipeline CI y Tests de Integración

## Technical Approach

Three independent but coordinated changes: (1) expand CI push triggers to all branches, (2) fix `@DynamicPropertySource` to satisfy `db.*` placeholders, (3) extract `spring.config.import` of AWS Secrets Manager into a dedicated `application-aws.yml` profile so tests never load it. Each change is independently revertable.

## Architecture Decisions

| Decision | Options | Tradeoff | Chosen |
|----------|---------|----------|--------|
| Push trigger scope | `branches: ['*']` vs no filter | Explicit wildcard adds noise with same behavior | **No filter** — clean, matches standard practice |
| AWS import isolation | `application-test.yml` with empty `spring.config.import` vs profile-separated `application-aws.yml` | Empty import doesn't work in SB 3.x (additive); profile separation is architecturally clean and propagates to all 4 services | **Profile: `application-aws.yml`** — clean separation, works with Spring Boot 3.3.6 additive import semantics |
| db.* override location | `@DynamicPropertySource` vs separate PropertySource | `@DynamicPropertySource` is idiomatic, co-located with Testcontainers lifecycle | **@DynamicPropertySource** — single source of truth for test infrastructure properties |
| Service scope | payment-service only vs all 4 services | All services use the same AWS import pattern (same `spring.config.import`, same `spring.cloud.aws.*` config); fixing all preemptively prevents the same CI failure when adding tests to other services | **All 4 services** — consistent architecture, no surprises |

## Data Flow

```
Test time (no aws profile active):         Production time (aws profile active):
┌──────────────────────────┐               ┌──────────────────────────┐
│ application.yml          │               │ application.yml          │
│   datasource.url: ${db.url}              │   datasource.url: ${db.url}
│   → NO spring.config.import│             │   → NO spring.config.import│
└──────────┬───────────────┘               └──────────┬───────────────┘
           │                                         │
           │ ┌───────────────────────┐               │ ┌───────────────────────┐
           │ │ application-aws.yml   │               │ │ application-aws.yml   │
           │ │   (NOT loaded)        │               │ │   spring.config.import │
           │ └───────────────────────┘               │ │   → aws-secretsmanager │
           │                                         │ │   → db.* from Secrets │
           │ ┌───────────────────────┐               │ └───────────────────────┘
           │ │ @DynamicPropertySource│               │         │
           │ │   → db.url from TC    │               │         ▼
           │ └───────────────────────┘               │ spring.datasource.url
           │         │                               │   = db.url from AWS
           ▼         ▼                               ▼
spring.datasource.url                             spring.datasource.url
  = db.url from Testcontainers                       = db.url from AWS Secrets
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `.github/workflows/backend-ci.yml` | Modify | Remove `branches` from `push` trigger |
| `.github/workflows/frontend-ci.yml` | Modify | Remove `branches` from `push` trigger |
| `.github/workflows/infra-ci.yml` | Modify | Add `push:` trigger with path filters |
| `backend/payment-service/src/test/.../PaymentIntegrationInfrastructure.java` | Modify | Add `db.url`, `db.username`, `db.password` to `@DynamicPropertySource` |
| `backend/payment-service/src/test/resources/application-test.yml` | Modify | Remove `spring.config.import: ""` hack (no longer needed; AWS import lives in profile) |
| `backend/payment-service/src/main/resources/application.yml` | Modify | Remove `spring.config.import` and `spring.cloud.aws.*` (moved to profile) |
| `backend/merchant-service/src/main/resources/application.yml` | Modify | Remove `spring.config.import` and `spring.cloud.aws.*` (moved to profile) |
| `backend/webhook-service/src/main/resources/application.yml` | Modify | Remove `spring.config.import` and `spring.cloud.aws.*` (moved to profile) |
| `backend/notification-service/src/main/resources/application.yml` | Modify | Remove `spring.config.import` and `spring.cloud.aws.*` (moved to profile) |
| `backend/*/src/main/resources/application-aws.yml` | Create | NEW — per-service profile with `spring.config.import` + `spring.cloud.aws.*`, activated via `SPRING_PROFILES_ACTIVE=aws` |

## Detailed Changes

### 1–3. CI Trigger fixes
No change. See previous version of this document.

### 4. DynamicPropertySource — `PaymentIntegrationInfrastructure.java`
No change. See previous version of this document.

### 5. AWS Profile Extraction — all 4 services

**Problem:** `spring.config.import: "optional:aws-secretsmanager:..."` lives in the base `application.yml`, so it loads unconditionally — even in tests. Spring Boot 3.x treats `spring.config.import` as additive, so a profile-specific override (`application-test.yml` with `import: ""`) cannot clear the base import. Tests that don't have access to AWS/LocalStack/Floci fail with `ApplicationContext` errors.

**Solution:** Extract all AWS-specific config into `application-aws.yml`, a profile-specific document activated only when `SPRING_PROFILES_ACTIVE=aws` is set.

**Before** — each `application.yml` had:
```yaml
spring:
  config:
    import: "optional:aws-secretsmanager:/payflow/.../db?prefix=db."
  cloud:
    aws:
      endpoint: http://localhost:4566
      region:
        static: us-east-1
      credentials:
        access-key: test
        secret-key: test
```

**After** — `application.yml` removes those lines, and a new `application-aws.yml` contains them:
```yaml
# application-aws.yml — activated via SPRING_PROFILES_ACTIVE=aws
spring:
  config:
    import: "optional:aws-secretsmanager:/payflow/${ENVIRONMENT:local}/.../db?prefix=db."
  cloud:
    aws:
      endpoint: http://localhost:4566
      region:
        static: us-east-1
      credentials:
        access-key: test
        secret-key: test
```

**Deployment requirement:** Environments that rely on AWS Secrets Manager (not K8s secrets or Docker Compose env vars) MUST set `SPRING_PROFILES_ACTIVE=aws`. The standard Docker Compose (`docker-compose.yml` and `docker-compose-floci.yml`) and K8s deployments override `SPRING_DATASOURCE_*` via environment variables and do NOT need the `aws` profile — the unresolved `${db.url}` placeholder is ignored when a higher-priority property (`SPRING_DATASOURCE_URL`) exists.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | CI trigger logic | Push a branch, verify workflow runs via `act` or GH API |
| Integration | `PaymentApiIntegrationTest` | Run with test profile — must load context and pass all assertions |
| Verify | infra CI | `terraform validate` on any branch push must succeed |

## Migration / Rollout

No migration required. All changes are configuration and test-scoped. Deploy in one PR; each file can be reverted independently.

## Open Questions

- [ ] Does `spring.config.import: ""` in a profile-specific YAML correctly clear the base property in Spring Boot 3.3.6? If YAML merge semantics keep the parent value, use `" "` (space) instead — Spring treats whitespace-only import as no-op.
