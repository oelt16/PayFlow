# Design: Phase 11 — Idempotency Keys

> **Source**: Engram observation #15

## Technical Approach

Filter-based idempotency that runs after `ApiKeyAuthenticationFilter`, using `ContentCachingRequestWrapper` + `ContentCachingResponseWrapper` to re-read the request body and capture the response body. Composite key `(merchant_id, idempotency_key)` with SHA-256 body hash.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Filter ordering | `@Order(HIGHEST_PRECEDENCE + 20)` | After auth (+10), MerchantContext available |
| Body buffering | `ContentCachingRequestWrapper` + custom wrapper | Enables multiple reads of input stream |
| Response caching | `ContentCachingResponseWrapper` | Post-controller capture, write-through |
| Transaction boundaries | `TransactionTemplate` with SERIALIZABLE | Atomic read-check-write, matches OutboxRelay pattern |
| TTL | 24h hardcoded | Simple, consistent, leverages DB index |

## Data Flow

```
Request → RequestIdFilter (+0)
       → ApiKeyAuthenticationFilter (+10) → sets MerchantContext
       → IdempotencyFilter (+20)
           ├─ No header → pass through
           ├─ Header + hash match → 200 + replayed header
           ├─ Hash mismatch → 422 idempotency_key_reuse
           └─ No match → proceed → store result atomically
       → Controller
```

## File Changes

| File | Action |
|------|--------|
| `V3__add_idempotency_keys.sql` | Create |
| `IdempotencyKeyJpaEntity.java` | Create |
| `IdempotencyKeySpringDataRepository.java` | Create |
| `IdempotencyService.java` + `IdempotencyResult.java` | Create |
| `IdempotencyFilter.java` | Create |
| `CachedBodyHttpServletRequest.java` | Create |
| `IdempotencyPurgeScheduler.java` | Create |
| `IdempotencyKeyReuseException.java` | Create |
| `ApiExceptionHandler.java` | Modify (add 422 handler) |
| `IdempotencyFilterTest.java` | Create |
| `IdempotencyServiceTest.java` | Create |

## Testing Strategy

| Layer | What to Test |
|-------|-------------|
| Unit | `IdempotencyService` hash/lookup/store logic |
| Unit | `IdempotencyFilter` path decisions (hit, miss, mismatch, no-header) |
| Unit | `IdempotencyPurgeScheduler` deletes expired |
| Integration | Duplicate POST → same paymentId |
| Integration | Same key + different body → 422 |
