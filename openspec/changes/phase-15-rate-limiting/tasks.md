# Tasks: Phase 15 — Rate Limiting

> **Source**: Engram observation #38

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 600–800 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (payment-service) → PR 2 (merchant-service mirror) |

## Phase 1: Foundation

- [ ] 1.1 Add `bucket4j-core` version to `backend/pom.xml`
- [ ] 1.2 Add `bucket4j-core` to payment-service `pom.xml`
- [ ] 1.3 Add `bucket4j-core` + `caffeine` to merchant-service `pom.xml`
- [ ] 1.4 Create `EndpointRateLimit.java` record
- [ ] 1.5 Create `RateLimitProperties.java` with `@ConfigurationProperties`
- [ ] 1.6 Add config to both `application.yml`

## Phase 2: Core Domain

- [ ] 2.1 Create `BucketRegistry.java` — Caffeine cache + Bucket4j factory
- [ ] 2.2 Create 429 response body helper

## Phase 3: Filter

- [ ] 3.1 Create `RateLimitFilter.java` at `HIGHEST_PRECEDENCE + 15`
- [ ] 3.2 Endpoint-specific limit matching
- [ ] 3.3 429 response with all 4 headers

## Phase 4: Testing (payment-service)

- [ ] 4.1 `RateLimitPropertiesTest` — defaults + YAML binding
- [ ] 4.2 `BucketRegistryTest` — create, evict, expire
- [ ] 4.3 `RateLimitFilterTest` — structural annotations
- [ ] 4.4 `RateLimitIntegrationTest` — 21st request = 429
- [ ] 4.5 Headers on 200 response
- [ ] 4.6 Headers on 429 response

## Phase 5: Mirror to merchant-service

- [ ] 5.1–5.6 Copy all source + test files with package rename
