# Proposal: Phase 15 — Rate Limiting

> **Source**: Engram observation #35

## Intent

Protect PayFlow APIs from abuse (card-testing, DDoS, runaway clients) with per-merchant token bucket rate limiting. Token bucket allows burst traffic while smoothing sustained load.

## Scope

- `bucket4j-core` in payment-service + merchant-service
- `RateLimitFilter` at `HIGHEST_PRECEDENCE + 15` (after auth, before idempotency)
- Default: 100 req/min, burst 20
- Strict: `POST /v1/payments` → 20/min, `POST /v1/merchants/me/api-keys` → 3/hour
- 429 with `X-RateLimit-*` headers + `Retry-After`
- Externalized config in `application.yml`
- In-memory Caffeine backend (no Redis)

## Approach

1. **Bucket4j + Caffeine**: Each `merchantId` → `Bucket` in ConcurrentHashMap with Caffeine eviction
2. **Filter ordering**: `+15` — after auth (`+10`), before idempotency (`+20`)
3. **Endpoint-specific**: URI + method matching against stricter limit registry
4. **429 response**: Standard error format + rate limit headers
