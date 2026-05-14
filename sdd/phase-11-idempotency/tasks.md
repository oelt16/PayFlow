# Tasks: Phase 11 — Idempotency Keys

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~500–700 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Notes |
|------|------|-------|
| 1 | Full idempotency feature | Migration + entity + repo + service + filter + tests + exception handler |

## Phase 1: Infrastructure (Foundation — DB + Entity + Repository)

- [ ] 1.1 Create `backend/payment-service/src/main/resources/db/migration/V3__add_idempotency_keys.sql` with `payments.idempotency_keys` table (key, merchant_id, request_hash, response_body JSONB, http_status, created_at, expires_at) and composite index `(merchant_id, expires_at)`
- [ ] 1.2 Create `backend/payment-service/src/main/java/com/payflow/payment/infrastructure/persistence/jpa/IdempotencyKeyJpaEntity.java` — JPA entity with `@Id`, `@Column` for all fields, `@JdbcTypeCode(SqlTypes.JSON)` on `responseBody`
- [ ] 1.3 Create `backend/payment-service/src/main/java/com/payflow/payment/infrastructure/persistence/jpa/IdempotencyKeySpringDataRepository.java` — extend `JpaRepository`, add `findByKeyAndMerchantIdAndExpiresAtAfter(String key, String merchantId, Instant now)`, delete method for purge

## Phase 2: Core Application Layer

- [ ] 2.1 Create `backend/payment-service/src/main/java/com/payflow/payment/application/exception/IdempotencyKeyReuseException.java` — custom `RuntimeException`, 422 error code `idempotency_key_reuse`
- [ ] 2.2 Create `backend/payment-service/src/main/java/com/payflow/payment/application/IdempotencyService.java` — `computeHash(byte[])` → SHA-256 lowercase hex; `lookup(merchantId, key, hash)` → `Optional<IdempotencyResult>`; `store(merchantId, key, hash, responseBody, status)` → void; use `TransactionTemplate` with `SERIALIZABLE` for read-check and `REQUIRED` for write; TTL = 24h from `Instant.now()`

## Phase 3: API Layer (Filter + Request Wrapper)

- [ ] 3.1 Create `backend/payment-service/src/main/java/com/payflow/payment/api/filter/CachedBodyHttpServletRequest.java` — `HttpServletRequestWrapper` that caches `getInputStream()` bytes into a `ByteArrayInputStream`, enabling multiple reads
- [ ] 3.2 Create `backend/payment-service/src/main/java/com/payflow/payment/api/filter/IdempotencyFilter.java` — `@Order(Ordered.HIGHEST_PRECEDENCE + 20)`, `OncePerRequestFilter`; extract `Idempotency-Key` header; compute SHA-256 of cached body; on cache hit: write cached response directly + `X-Idempotent-Replayed: true` + `Cache-Control: no-store`, skip chain; on hash mismatch: throw `IdempotencyKeyReuseException`; on miss: wrap request in `CachedBodyHttpServletRequest`, wrap response in `ContentCachingResponseWrapper`, proceed; after chain: capture cached response body, call `idempotencyService.store()`, copy body to real response, call `copyBodyToResponse()`

## Phase 4: Exception Handler

- [ ] 4.1 Add `IdempotencyKeyReuseException` handler to `backend/payment-service/src/main/java/com/payflow/payment/api/ApiExceptionHandler.java` → returns `HttpStatus.UNPROCESSABLE_ENTITY` with error code `idempotency_key_reuse`

## Phase 5: Scheduler (Cleanup)

- [ ] 5.1 Create `backend/payment-service/src/main/java/com/payflow/payment/infrastructure/scheduler/IdempotencyPurgeScheduler.java` — `@Scheduled(cron = "0 0 2 * * *")` (daily 2am), inject repository, call `deleteByExpiresAtBefore(Instant.now())`, log count

## Phase 6: Testing (TDD)

- [ ] 6.1 RED: Write `IdempotencyServiceTest` — test `computeHash` output is 64-char lowercase hex; test `lookup` returns empty on no entry; test `lookup` returns result on match; test `store` persists entity with correct TTL
- [ ] 6.2 RED: Write `IdempotencyFilterTest` — mock `IdempotencyService`; verify: no header → chain proceeds, hash mismatch → `IdempotencyKeyReuseException` thrown, cache hit → 200 written + `X-Idempotent-Replayed` header set, cache miss → chain proceeds and `store()` called after
- [ ] 6.3 GREEN: Implement `IdempotencyService` until all `IdempotencyServiceTest` tests pass
- [ ] 6.4 GREEN: Implement `IdempotencyFilter` until all `IdempotencyFilterTest` tests pass
- [ ] 6.5 RED: Write `IdempotencyIntegrationTest` — real DB, RestTemplate; verify: duplicate POST with same key returns same `paymentId`, same key + different body returns 422, no header → normal 201 flow
- [ ] 6.6 GREEN: Get all integration tests passing

## Dependency Order

Phase 1 (DB + Entity + Repo) → Phase 2 (Service) → Phase 3 (Filter + Wrapper) → Phase 4 (Exception Handler) → Phase 5 (Scheduler) → Phase 6 (Tests). Phase 3 depends on Phase 2. Phase 4 depends on Phase 2.