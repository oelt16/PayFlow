# Tasks: Fix Pipeline CI y Tests de Integración

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~13 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

## Phase 1: CI Trigger Fixes

- [x] 1.1 Remove `branches: [main, master]` from `push` trigger in `.github/workflows/backend-ci.yml` — keep path filters and Docker `if:` guard intact
- [x] 1.2 Remove `branches: [main, master]` from `push` trigger in `.github/workflows/frontend-ci.yml` — keep path filters and Docker `if:` guard intact
- [x] 1.3 Add `push:` trigger with path filters to `.github/workflows/infra-ci.yml` — matches existing `pull_request` path filters

## Phase 2: Test Configuration Fixes (TDD: GREEN)

- [x] 2.1 Create `backend/payment-service/src/test/resources/application-test.yml` with `spring.config.import: ""` to disable AWS Secrets Manager config loading
- [x] 2.2 Add `registry.add("db.url", ...)`, `registry.add("db.username", ...)`, `registry.add("db.password", ...)` to `@DynamicPropertySource` in `PaymentIntegrationInfrastructure.java` — same Testcontainers values as `spring.datasource.*`

## Phase 3: Verification

- [x] 3.1 Run `./mvnw -B verify -pl payment-service` — confirm `PaymentApiIntegrationTest` loads context and passes all assertions

## Dependency Graph

```
1.1 ─┐
1.2 ─┤──→ (independent, any order)
1.3 ─┘
2.1 ───→ 2.2 (2.1 blocks 2.2 if test profile loading fails)
2.1 ─┐
2.2 ─┴─→ 3.1 (both required before verification)
```
