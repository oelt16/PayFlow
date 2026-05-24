# Tasks: Phase 11 — Idempotency Keys

> **Source**: Engram observation #16

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~500–700 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |

## Phase 1: Infrastructure (DB + Entity + Repository)

- [ ] 1.1 Create `V3__add_idempotency_keys.sql` Flyway migration
- [ ] 1.2 Create `IdempotencyKeyJpaEntity.java` JPA entity
- [ ] 1.3 Create `IdempotencyKeySpringDataRepository.java`

## Phase 2: Core Application Layer

- [ ] 2.1 Create `IdempotencyKeyReuseException.java` (422 error)
- [ ] 2.2 Create `IdempotencyService.java` — computeHash, lookup, store; 24h TTL

## Phase 3: API Layer (Filter + Request Wrapper)

- [ ] 3.1 Create `CachedBodyHttpServletRequest.java` — wrapper for multiple body reads
- [ ] 3.2 Create `IdempotencyFilter.java` — @Order(HIGHEST_PRECEDENCE + 20)

## Phase 4: Exception Handler

- [ ] 4.1 Add `IdempotencyKeyReuseException` handler to `ApiExceptionHandler.java` → 422

## Phase 5: Scheduler

- [ ] 5.1 Create `IdempotencyPurgeScheduler.java` — @Scheduled(cron = "0 0 2 * * *")

## Phase 6: Testing (TDD)

- [ ] 6.1 RED → GREEN: `IdempotencyServiceTest` — computeHash, lookup, store
- [ ] 6.2 RED → GREEN: `IdempotencyFilterTest` — no-header, hash mismatch, cache hit, miss
- [ ] 6.3 RED → GREEN: `IdempotencyIntegrationTest` — duplicate POST, 422 body mismatch
