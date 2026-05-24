# Apply Progress: Phase 11 — Idempotency Keys

> **Source**: Engram observation #17

## Completed Tasks

### Phase 1: Infrastructure
- ✅ 1.1 `V3__add_idempotency_keys.sql` — Flyway migration
- ✅ 1.2 `IdempotencyKeyJpaEntity.java` — JPA entity with JSONB
- ✅ 1.3 `IdempotencyKeySpringDataRepository.java` — custom queries

### Phase 2: Application Layer
- ✅ 2.1 `IdempotencyKeyReuseException.java`
- ✅ 2.2 `IdempotencyService.java` + `IdempotencyResult.java` — 24h TTL

### Phase 3: API Layer
- ✅ 3.1 `CachedBodyHttpServletRequest.java` — body caching wrapper
- ✅ 3.2 `IdempotencyFilter.java` — @Order(HIGHEST_PRECEDENCE + 20)

### Phase 4: Exception Handler
- ✅ 4.1 `ApiExceptionHandler.java` — 422 handler added

### Phase 5: Scheduler
- ✅ 5.1 `IdempotencyPurgeScheduler.java` — daily 2am UTC

### Phase 6: Tests
- ✅ 6.1–6.6 All 34 tests passing

## Key Implementation Details
- SHA-256 lowercase hex hash (64 chars)
- 24-hour TTL for idempotency keys
- JSONB for response body storage
- `X-Idempotent-Replayed` header on cache hit
