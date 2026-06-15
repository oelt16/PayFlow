# Proposal: Fix Pipeline CI y Tests de Integración

## Intent

Three CI issues block productive development on feature branches:
1. All 3 workflows only trigger on `main`/`master` — zero CI feedback on feature branches
2. `PaymentApiIntegrationTest` can't load Spring context in CI after Secrets Manager migration — `@DynamicPropertySource` sets `spring.datasource.*` but not `db.*`
3. `infra-ci.yml` is missing `push` trigger entirely — infra changes get no validation until PR

## Scope

### In Scope
- Add `push:` (no branch filter) to all 3 CI workflows, keeping existing path filters
- Fix `PaymentIntegrationInfrastructure.java` — add `db.url`, `db.username`, `db.password` to `@DynamicPropertySource`
- Create `application-test.yml` in payment-service to disable Secrets Manager import as safety net

### Out of Scope
- Adding `push:` to Docker build jobs (they correctly gate on `main`/`master` only)
- Fixing other services' test configurations (only payment-service affected)
- Refactoring CI workflow structure or adding new CI steps

## Capabilities

### New Capabilities
- `ci-test-configuration`: Test configuration overrides for CI — `application-test.yml`, `@DynamicPropertySource` patterns for Secrets Manager placeholders, decoupling tests from external AWS dependencies.

### Modified Capabilities
- `infrastructure-provisioning`: CI Workflow requirement expands from "every PR" to "every PR and every push on any branch" to give feature branches early feedback.

## Approach

1. **CI triggers** — Remove `branches: [main, master]` from `push` triggers in all 3 `.yml` files. Keep `paths` filters. Leave `pull_request` triggers and Docker `if:` guards unchanged.
2. **DynamicPropertySource** — Add `registry.add("db.url", ...)`, `db.username`, `db.password` using the same Testcontainers values already set for `spring.datasource.*`.
3. **application-test.yml** — Create test profile config that overrides `spring.config.import` to skip `aws-secretsmanager`, removing the external dependency at the config level.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `.github/workflows/backend-ci.yml` | Modified | Remove `branches` from `push` trigger |
| `.github/workflows/frontend-ci.yml` | Modified | Remove `branches` from `push` trigger |
| `.github/workflows/infra-ci.yml` | Modified | Add `push:` trigger matching `pull_request` paths |
| `backend/payment-service/src/test/java/.../PaymentIntegrationInfrastructure.java` | Modified | Add `db.*` properties to `@DynamicPropertySource` |
| `backend/payment-service/src/test/resources/application-test.yml` | New | Disable Secrets Manager import for test profile |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| CI runs twice (push + PR) for same commit | Low | Path filters still apply; duplicate runs are acceptable for early feedback |
| Docker build runs on every push | Low | Docker `if:` guard already checks `github.ref == main/master` — unchanged |
| Other test configs also missing `db.*` | Low | Only payment-service uses `?prefix=db.` pattern; merchant/webhook use flat `spring.datasource.*` as prefix |

## Rollback Plan

- **CI triggers**: Revert the `push:` line changes in each workflow file
- **Test fixes**: Revert `PaymentIntegrationInfrastructure.java` changes, delete `application-test.yml`
- If tests break on a different service, revert per-service independently — changes are isolated

## Dependencies

- CI will need Testcontainers support (Docker-in-Docker) for integration tests — already configured in workflow runner
- No external dependency changes

## Success Criteria

- [ ] Pushing to any feature branch triggers backend, frontend, and infra CI within 30s
- [ ] `PaymentApiIntegrationTest` passes in CI (no `ApplicationContext` load failure)
- [ ] `infra-ci.yml` runs `terraform validate` on push to feature branches
