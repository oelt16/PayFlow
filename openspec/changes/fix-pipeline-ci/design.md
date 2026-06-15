# Design: Fix Pipeline CI y Tests de Integración

## Technical Approach

Three independent but coordinated changes: (1) expand CI push triggers to all branches, (2) fix `@DynamicPropertySource` to satisfy `db.*` placeholders, (3) add `application-test.yml` to decouple tests from AWS Secrets Manager at config load time. Each change is independently revertable.

## Architecture Decisions

| Decision | Option | Tradeoff | Chosen |
|----------|--------|----------|--------|
| Push trigger scope | `branches: ['*']` vs no filter | Explicit wildcard adds noise with same behavior | **No filter** — clean, matches standard practice |
| disable AWS import | `spring.config.import: ""` vs `-Dspring.cloud.aws.secretsmanager.enabled=false` | Flag only disables AWS extension; import still resolves | **Empty import** — kills config loading at the source, no AWS SDK interaction at all |
| db.* override location | `@DynamicPropertySource` vs separate PropertySource | `@DynamicPropertySource` is idiomatic, co-located with Testcontainers lifecycle | **@DynamicPropertySource** — single source of truth for test infrastructure properties |

## Data Flow

```
application.yml                    application-test.yml (test profile)
┌──────────────────────┐           ┌──────────────────────┐
│ spring.config.import │           │ spring.config.import │
│   → aws-secretsmanager│  OVERRIDE│   → "" (empty)       │
│   → loads db.* props │  ──────►  │   → no AWS SDK touch │
└──────────┬───────────┘           └──────────────────────┘
           │                                │
           │ Without fix: env missing db.*   │ With fix: skip AWS, use:
           │   → ${db.url} unresolved        │   @DynamicPropertySource
           │   → ApplicationContext fails    │   → registry.add("db.url", ...)
           ▼                                ▼
    spring.datasource.url = ${db.url}     spring.datasource.url = postgres://...
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `.github/workflows/backend-ci.yml` | Modify | Remove `branches` from `push` trigger |
| `.github/workflows/frontend-ci.yml` | Modify | Remove `branches` from `push` trigger |
| `.github/workflows/infra-ci.yml` | Modify | Add `push:` trigger with path filters |
| `backend/payment-service/src/test/.../PaymentIntegrationInfrastructure.java` | Modify | Add `db.url`, `db.username`, `db.password` to `@DynamicPropertySource` |
| `backend/payment-service/src/test/resources/application-test.yml` | Create | Override `spring.config.import` to empty, disabling AWS Secrets Manager |

## Detailed Changes

### 1. CI Trigger — `backend-ci.yml`

**Before:**
```yaml
on:
  push:
    branches: [main, master]    # ← remove this line
    paths:
      - 'backend/**'
      - '.github/workflows/backend-ci.yml'
```

**After:**
```yaml
on:
  push:
    paths:
      - 'backend/**'
      - '.github/workflows/backend-ci.yml'
```

Docker `if:` guard (line 49) is untouched:
```yaml
if: github.event_name == 'push' && (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master')
```

### 2. CI Trigger — `frontend-ci.yml`

**Before:**
```yaml
on:
  push:
    branches: [main, master]    # ← remove this line
    paths:
      - 'frontend/**'
```

**After:**
```yaml
on:
  push:
    paths:
      - 'frontend/**'
```

Docker `if:` guard (line 54) unchanged.

### 3. CI Trigger — `infra-ci.yml`

**Before:**
```yaml
on:
  pull_request:
    branches: [main, master]
    paths:
      - 'infra/terraform/**'
      - '.github/workflows/infra-ci.yml'
```

**After:**
```yaml
on:
  push:
    paths:
      - 'infra/terraform/**'
      - '.github/workflows/infra-ci.yml'
  pull_request:
    branches: [main, master]
    paths:
      - 'infra/terraform/**'
      - '.github/workflows/infra-ci.yml'
```

### 4. DynamicPropertySource — `PaymentIntegrationInfrastructure.java`

**Before** (lines 37-39):
```java
registry.add("spring.datasource.url", CONTAINER1_POSTGRES::getJdbcUrl);
registry.add("spring.datasource.username", CONTAINER1_POSTGRES::getUsername);
registry.add("spring.datasource.password", CONTAINER1_POSTGRES::getPassword);
```

**After** (adds 3 lines):
```java
registry.add("spring.datasource.url", CONTAINER1_POSTGRES::getJdbcUrl);
registry.add("spring.datasource.username", CONTAINER1_POSTGRES::getUsername);
registry.add("spring.datasource.password", CONTAINER1_POSTGRES::getPassword);
registry.add("db.url", CONTAINER1_POSTGRES::getJdbcUrl);
registry.add("db.username", CONTAINER1_POSTGRES::getUsername);
registry.add("db.password", CONTAINER1_POSTGRES::getPassword);
```

These satisfy the `{db.url}`, `${db.username}`, `${db.password}` placeholders in `application.yml`'s datasource block (lines 22-24).

### 5. New File — `application-test.yml`

Full content:

```yaml
spring:
  config:
    import: ""
```

**What it does:** The test profile (`@ActiveProfiles("test")` or default `test`) overrides `spring.config.import` from `application.yml` (line 8: `"optional:aws-secretsmanager:/payflow/.../db?prefix=db."`). By setting it to `""`, Spring Boot skips the AWS Secrets Manager import entirely, so tests never attempt an AWS SDK connection. The `@DynamicPropertySource` provides all database properties directly.

**Why not just `@DynamicPropertySource`?** The `spring.config.import` is resolved *before* `@DynamicPropertySource` runs — even with `optional:`, the AWS SDK can throw during PropertySource resolution if credentials are missing. The `application-test.yml` ensures config loading itself succeeds, and `@DynamicPropertySource` fills in the actual values.

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
