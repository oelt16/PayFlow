# Specification: API Rate Limiting

> **Source**: Engram observation #36

## Requirements

### RLM-1: Default Rate Limit
- 100 req/min default, burst 20 — token bucket per merchant
- Configurable via `payflow.rate-limit.requests-per-minute` and `burst-capacity`
- 21st rapid request → 429 with `Retry-After > 0`

### RLM-2: Stricter Endpoint Limits
| Endpoint | Method | Limit |
|----------|--------|-------|
| `/v1/payments` | POST | 20/min |
| `/v1/merchants/me/api-keys` | POST | 3/hour |

### RLM-3: Rate Limit Headers
Every response includes:
- `X-RateLimit-Limit` — max requests per window
- `X-RateLimit-Remaining` — tokens remaining
- `X-RateLimit-Reset` — Unix timestamp of refill
- `Retry-After` — seconds (only on 429)

### RLM-4: Externalized Configuration
```yaml
payflow:
  rate-limit:
    requests-per-minute: 100
    burst-capacity: 20
    endpoints:
      - path: /v1/payments, method: POST, tokens: 20, period: 60s
      - path: /v1/merchants/me/api-keys, method: POST, tokens: 3, period: 3600s
```

### RLM-5: In-Memory Backend
- Caffeine cache with `maximumSize` (10,000) + `expireAfterAccess` (30 min)
- Evicted merchants get fresh bucket on next request

### RLM-6: Filter Ordering
- After auth (+10), before idempotency (+20)
- Unauthenticated requests never reach filter
- Rate-limited requests don't consume idempotency storage
