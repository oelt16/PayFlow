# Phase 15 — Rate Limiting

Per-merchant token bucket rate limiting is now enforced on all API requests. Requests that exceed the limit receive a `429 Too Many Requests` response with standard rate limit headers. This protects the platform from abuse (card-testing attacks, DDoS) and demonstrates production-grade API security.

## Quick path

1. Start the stack: `docker compose up -d`
2. Send 25 rapid requests via browser console, Bruno, or curl
3. Observe: first ~20 succeed, remaining return `429` with `Retry-After` header
4. Check logs: `docker compose logs payment-service | grep -i rate`

## What

| Topic | Decision |
|-------|----------|
| **Pattern** | Token bucket per API key (not fixed window) — handles bursts gracefully |
| **Backend** | Bucket4j 8.18.0 + Caffeine in-memory cache |
| **Services** | `payment-service` and `merchant-service` (each with its own bucket registry) |
| **Default limit** | 100 requests/minute, burst capacity of 20 tokens |
| **Stricter limits** | `POST /v1/payments` → 20/min (card-testing protection), `POST /v1/merchants/me/api-keys` → 3/hour (key rotation abuse) |
| **Filter order** | `HIGHEST_PRECEDENCE + 15` — after auth (`+10`), before idempotency (`+20`) |
| **No Redis** | In-memory only for Phase 15. Distributed rate limiting deferred. |

## Why

- **Security**: Rate limiting is table-stakes for any public payment API. It protects against card-testing attacks where fraudsters probe whether card numbers are valid by submitting many small payments rapidly.
- **Interview signal**: Every fintech engineering interview at a bank or neobank asks "how do you protect this in production?". Token bucket is the right answer over fixed window because it handles burst traffic gracefully.
- **Completes the spec**: Phase 15 was the final planned phase. All 15 phases of PayFlow are now delivered.

## How it works

```
Request → Auth filter (+10) → RateLimitFilter (+15) → Idempotency filter (+20) → Controller
                                    │
                                    ├─ Look up or create Bucket for merchantId (Caffeine cache)
                                    ├─ Resolve endpoint-specific limit (method + path match)
                                    ├─ Try to consume 1 token
                                    │   ├─ Success → add rate limit headers, continue chain
                                    │   └─ Exhausted → return 429 with Retry-After
                                    └─ Skip if: disabled, non-/v1/ path, or no merchant context
```

### Files added

| Service | Source files | Test files |
|---------|-------------|------------|
| `payment-service` | `RateLimitFilter`, `BucketRegistry`, `RateLimitConfig`, `RateLimitProperties`, `RateLimitResponse`, `EndpointRateLimit` | `RateLimitFilterTest`, `BucketRegistryTest`, `RateLimitPropertiesTest`, `RateLimitIntegrationTest`, `RateLimitTestController` |
| `merchant-service` | Same 6 files (package: `com.payflow.merchant.api.ratelimit`) | Same 5 test files |

### Configuration

Both services expose rate limits via `application.yml`:

```yaml
payflow:
  rate-limit:
    enabled: true
    requests-per-minute: 100
    burst-capacity: 20
    cache-max-size: 10000
    cache-ttl: 30m
    endpoints:
      payments-create:
        method: POST
        path: /v1/payments
        tokens: 20
        refill-duration: 1m
```

Merchant-service uses `3/hour` for `POST /v1/merchants/me/api-keys` instead.

### 429 Response format

```json
{
  "error": {
    "code": "rate_limit_exceeded",
    "message": "Rate limit exceeded. Please retry after 45 seconds.",
    "requestId": "req_abc123"
  }
}
```

Headers on every response:

| Header | Present on | Meaning |
|--------|-----------|---------|
| `X-RateLimit-Limit` | 200 + 429 | Max requests in the window |
| `X-RateLimit-Remaining` | 200 + 429 | Tokens left (0 on 429) |
| `X-RateLimit-Reset` | 200 + 429 | Epoch seconds when bucket refills |
| `Retry-After` | 429 only | Seconds to wait before retrying |

## How to test

### Option 1: Browser console (easiest — uses the frontend)

Open the PayFlow dashboard in your browser, then paste in DevTools console:

```javascript
// Fire 25 rapid requests to trigger the 20/min limit on POST /v1/payments
const results = [];
for (let i = 1; i <= 25; i++) {
  fetch('/api/v1/payments', {
    headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('apiKey') || 'YOUR_KEY_HERE') }
  })
  .then(r => {
    results.push({ req: i, status: r.status, remaining: r.headers.get('X-RateLimit-Remaining') });
    if (i === 25) console.table(results);
  });
}
```

**Expected**: First ~20 return `200`/`201` with decreasing `remaining`, then the rest return `429` with `remaining: 0`.

### Option 2: curl

```bash
# Linux / macOS
for i in $(seq 1 25); do
  curl -s -o /dev/null -w "Request $i: HTTP %{http_code}\n" \
    -H "Authorization: Bearer sk_test_your_key" \
    http://localhost:8081/v1/payments
done

# Windows PowerShell
1..25 | ForEach-Object {
  $r = Invoke-WebRequest -Uri "http://localhost:8081/v1/payments" `
    -Headers @{ "Authorization" = "Bearer sk_test_your_key" } `
    -Method GET -UseBasicParsing
  Write-Host "Request $_: HTTP $($r.StatusCode) | Remaining: $($r.Headers['X-RateLimit-Remaining'])"
}
```

### Option 3: Check logs

```bash
# Linux / macOS
docker compose logs payment-service | grep -i rate

# Windows PowerShell
docker compose logs payment-service | Select-String -Pattern "rate"
```

Look for WARN-level entries when a merchant hits the limit.

### Option 4: Check the database (indirect)

Rate limiting is **fully in-memory** — there is no DB table for it. But you can verify indirectly:

```bash
# Linux / macOS
docker compose exec postgres psql -U payflow -d payflow -c \
  "SELECT count(*) as total_payments FROM payments;"

# Windows PowerShell
docker compose exec postgres psql -U payflow -d payflow -c "SELECT count(*) as total_payments FROM payments;"
```

Run this before and after a burst. The count should NOT increase by 25 — only the requests that passed rate limiting will create payments. Rate-limited requests never reach the application layer, so they don't touch the DB.

### Option 5: Run unit + integration tests

```bash
cd backend

# Linux / macOS
./mvnw test -pl payment-service,merchant-service -Dtest="*RateLimit*"

# Windows PowerShell
cd backend; ./mvnw test -pl payment-service,merchant-service -Dtest="*RateLimit*"
```

**Expected**: ~39 tests pass across both services.

## Checklist

- [ ] Rate limiting activates after burst capacity is exhausted
- [ ] 429 response includes all four headers (`Limit`, `Remaining`, `Reset`, `Retry-After`)
- [ ] Stricter limit on `POST /v1/payments` (20/min) vs default (100/min)
- [ ] Different merchants get separate buckets (one merchant limited doesn't affect others)
- [ ] Non-`/v1/` paths bypass rate limiting (health checks, actuator)
- [ ] Unauthenticated requests bypass rate limiting (auth filter runs first)
- [ ] Logs show rate limit events when threshold is hit

## Next step

All 15 phases of PayFlow are complete. No remaining planned work. Optional improvements: Redis-based distributed rate limiting, admin dashboard for per-merchant limit overrides, Micrometer metrics for rate limit hits/misses.
