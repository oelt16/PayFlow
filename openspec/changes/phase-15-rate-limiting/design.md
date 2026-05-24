# Design: Phase 15 — Rate Limiting

> **Source**: Engram observation #37

## Technical Approach

Per-merchant token bucket rate limiting at the servlet filter layer using Bucket4j + Caffeine in-memory store. Filter at `HIGHEST_PRECEDENCE + 15`.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Algorithm | Token bucket (Bucket4j) | Handles burst traffic gracefully |
| Backend | Caffeine in-memory | Simple, fast; lost on restart (acceptable) |
| Config binding | `@ConfigurationProperties` | Type-safe, follows `api-key-cache` pattern |
| Endpoint matching | Exact path+method | Simple, extendable to prefix later |
| Module structure | Per-service copy | Matches existing auth filter pattern |

## Data Flow

```
Request → RequestIdFilter (+0)
       → AuthFilter (+10) — sets MerchantContext
       → RateLimitFilter (+15) — checks bucket, sets headers
           ├─ tryConsume(1) → true: continue chain
           └─ tryConsume(1) → false: 429 + headers
       → IdempotencyFilter (+20)
       → Controller
```

## File Changes (per service)

| File | Action |
|------|--------|
| `RateLimitFilter.java` | Create — `OncePerRequestFilter` at order +15 |
| `RateLimitProperties.java` | Create — `@ConfigurationProperties` |
| `BucketRegistry.java` | Create — Caffeine cache + Bucket factory |
| `EndpointRateLimit.java` | Create — method, path, tokens, refillDuration |
| `RateLimitConfig.java` | Create — `FilterRegistrationBean` |

Mirrored to both payment-service and merchant-service.

## Testing Strategy

| Layer | Approach |
|-------|----------|
| Unit | Filter order, `@Component`/`FilterRegistrationBean` |
| Unit | BucketRegistry: creation, cache eviction |
| Integration | 21st request → 429 with headers |
| Integration | Retry-After > 0 on 429 |
| Integration | Headers present on 200 responses |
