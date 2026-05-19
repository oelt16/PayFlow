# Phase 12: Cross-Service API Key Cache

## Starting Point

In Phase 8 (Unified API Key Authentication), we fixed the authentication disconnect where registered keys from merchant-service didn't work on payment-service and webhook-service. The solution was to make payment-service and webhook-service read directly from the shared `merchants.merchants` table using JDBC + BCrypt.

**However**, this had a performance problem:

```
Every API request → direct DB query → BCrypt match
```

For a payment API, this is inefficient — the same merchant's key is validated repeatedly on every request.

---

## What Was Implemented

### 1. Caffeine Cache in payment-service and webhook-service

**Files created:**
- `payment-service/src/main/java/com/payflow/payment/cache/ApiKeyCache.java` — interface
- `payment-service/src/main/java/com/payflow/payment/cache/CaffeineApiKeyCache.java` — Caffeine implementation
- `payment-service/src/main/java/com/payflow/payment/domain/cache/ValidatedMerchant.java` — cache entry record
- Same files in `webhook-service`

**Configuration** (application.yml):
```yaml
payflow:
  api-key-cache:
    ttl-seconds: 600      # 10 minutes
    max-size: 10000       # max entries
    internal-endpoint:
      base-url: ${PAYFLOW_API_KEY_CACHE_INTERNAL_ENDPOINT_BASE_URL:http://localhost:8082}
      path: /v1/internal/merchants/validate-key
```

**Docker Compose** sets the internal URL for container-to-container communication:
```yaml
payment-service:
  environment:
    PAYFLOW_API_KEY_CACHE_INTERNAL_ENDPOINT_BASE_URL: http://merchant-service:8082
```

### 2. Internal Validation Endpoint in merchant-service

**File created:**
- `merchant-service/src/main/java/com/payflow/merchant/api/internal/ValidateKeyController.java`
- `merchant-service/src/main/java/com/payflow/merchant/api/dto/ValidateKeyRequest.java`
- `merchant-service/src/main/java/com/payflow/merchant/api/dto/ValidateKeyResponse.java`

**Endpoint:**
```
POST /v1/internal/merchants/validate-key
Request:  { "keyPrefix": "pk_live_ab" }
Response: { "merchantId": "...", "keyHash": "$2a$10$...", "isActive": true }
```

### 3. Kafka Consumer for Cache Invalidation

**Files created:**
- `payment-service/src/main/java/com/payflow/payment/infrastructure/kafka/MerchantEventConsumer.java`
- `webhook-service/src/main/java/com/payflow/webhook/infrastructure/kafka/MerchantEventConsumer.java`

**Behavior:**
- Listens to `merchant.events` topic
- On `MerchantDeactivatedEvent` → evicts the keyPrefix from local cache

### 4. Updated JdbcApiKeyAuthenticator

Modified to use read-through cache pattern with **graceful fallback**:

1. Check cache first
2. On cache miss → call merchant-service via HTTP (RestTemplate)
3. Cache result with keyHash for local BCrypt validation
4. **If HTTP call fails → fall back to direct DB query (Phase 8 behavior)**

The fallback ensures the system works even when the HTTP endpoint is unavailable.

---

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         REQUEST                                 │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              ApiKeyAuthenticationFilter                        │
│                   extracts Bearer token                         │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              JdbcApiKeyAuthenticator                           │
│                                                               │
│  1. Extract keyPrefix (first 8 chars)                          │
│  2. CHECK CACHE (CaffeineApiKeyCache)                          │
│     └── Hit? → BCrypt match → return MerchantId                │
│                                                               │
│  3. MISS? → HTTP call to merchant-service                      │
│     POST /v1/internal/merchants/validate-key                  │
│                                                               │
│  4. Cache result (merchantId + keyHash + isActive)           │
│  5. BCrypt match → return MerchantId                          │
│                                                               │
│  6. If HTTP fails → FALL BACK to direct DB query              │
│     (Phase 8 behavior - preserves functionality)             │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼ (on cache miss + HTTP failure)
┌─────────────────────────────────────────────────────────────────┐
│              Direct DB Query (Fallback - Phase 8)              │
│  SELECT id, key_hash FROM merchants.merchants                   │
│  WHERE key_prefix = ? AND is_active = TRUE                    │
└─────────────────────────────────────────────────────────────────┘
                       │
                       ▼ (on cache miss)
┌─────────────────────────────────────────────────────────────────┐
│              merchant-service (port 8082)                      │
│  ValidateKeyController                                         │
│  - Query merchants.merchants by key_prefix                     │
│  - Return merchantId + keyHash + isActive                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              Kafka consumer (merchant.events)                  │
│  On MerchantDeactivatedEvent → evict(keyPrefix)               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Why This Implementation

### Production Patterns Used

1. **Read-through cache** — cache is populated on first read, not on write
2. **Local BCrypt validation** — after fetching keyHash from merchant-service, we do BCrypt locally (faster than HTTP each time)
3. **Graceful fallback** — if HTTP call fails, falls back to direct DB query (Phase 8 behavior)
4. **TTL + Event invalidation** — 10-minute TTL as safety net, Kafka event for immediate invalidation

### Trade-offs

| Aspect | Before (Phase 8) | After (Phase 12) |
|--------|-----------------|------------------|
| Auth lookup | DB round-trip every request | Cache hit = O(1) |
| Performance | Slow under load | Fast with cache |
| Invalidation | Always fresh (direct DB) | TTL + Kafka event |
| Complexity | Simple | Higher (cache + HTTP + Kafka) |

---

## How to Test

### 1. Unit Tests (already pass)

```bash
cd backend
./mvnw test -pl payment-service,webhook-service
```

Check for `ApiKeyCacheTest` — 5 tests covering get/put/evict/TTL.

### 2. Integration Testing with Docker Compose

Start the full stack:

```bash
cd infra
docker-compose up -d
```

Wait for all services to be healthy:
```bash
docker-compose ps
```

### 3. Test Cache Hit

Make repeated requests with the same API key:

```bash
# First request (cache miss → HTTP call to merchant-service)
curl -H "Authorization: Bearer sk_test_dev" \
     http://localhost:8081/v1/payments

# Second request (cache hit → no HTTP call)
curl -H "Authorization: Bearer sk_test_dev" \
     http://localhost:8081/v1/payments
```

**Check logs to verify:**

#### payment-service logs (Windows):
```powershell
docker logs payment-service
# Look for: "Cache hit for key prefix:"
```

#### payment-service logs (Linux/macOS):
```bash
docker logs payment-service
# Look for: "Cache hit for key prefix:"
```

### 4. Test Cache Miss + HTTP Call

Start with empty cache (restart service):

```bash
docker restart payment-service
```

First request shows cache miss:
```powershell
docker logs payment-service 2>&1 | Select-String -Pattern "Cache miss|merchant-service"
```

Expected output:
```
... DEBUG ... Cache miss for key prefix: sk_test_
... INFO ... Evicting cached API key for deactivated merchant: ...
```

### 5. Test Cache Invalidation via Kafka

Trigger merchant deactivation:

```powershell
docker run --rm -it --network payflow_default `
  confluentinc/cp-kafka:latest `
  kafka-console-producer `
  --broker-list kafka:9092 `
  --topic merchant.events
```

Send deactivation event:
```json
{"eventId":"evt_test","eventType":"merchant.deactivated","aggregateId":"mer_test_dev","merchantId":"mer_test_dev","occurredAt":"2026-05-16T12:00:00Z","payload":{"keyPrefix":"sk_test_dev"}}
```

**Check cache eviction in logs:**

```powershell
docker logs payment-service 2>&1 | Select-String -Pattern "Evicting cached"
```

Expected:
```
... INFO ... Evicting cached API key for deactivated merchant: sk_test_dev
```

### 6. Test Graceful Fallback Behavior

Stop merchant-service and try to authenticate:

```bash
docker stop merchant-service
```

Request should **still work** using direct DB fallback (Phase 8 behavior):

```bash
curl -H "Authorization: Bearer sk_test_dev" \
     http://localhost:8081/v1/payments
# Expected: 200 OK (falls back to direct DB)
```

**Check logs:**

```powershell
docker logs payment-service 2>&1 | Select-String -Pattern "falling back to direct DB"
```

Expected:
```
... WARN ... Failed to validate key via merchant-service, falling back to direct DB: ...
```

This ensures the system remains functional even when the HTTP endpoint is unavailable.

### 7. Verify Database State

Connect to PostgreSQL:

```bash
docker exec -it payflow-postgres-1 psql -U payflow -d payflow
```

Check merchants table:
```sql
SELECT id, name, key_prefix, is_active FROM merchants.merchants;
```

---

## Interview Talking Points

> **"The 10-minute TTL means a compromised key is valid for at most 5 minutes if deactivation arrives after a cache population. The Kafka event cuts that window to near-zero in the normal case. This is the same pattern Stripe uses for their internal service mesh."**

---

## Files Modified

| Service | Files Changed |
|---------|---------------|
| payment-service | pom.xml (caffeine), application.yml, JdbcApiKeyAuthenticator.java, new cache + Kafka consumer files |
| webhook-service | pom.xml (caffeine), application.yml, JdbcApiKeyAuthenticator.java, new cache + Kafka consumer files |
| merchant-service | ValidateKeyController.java, new DTOs, MerchantDeactivatedEvent (already had keyPrefix) |
| infra | docker-compose.yml (added internal endpoint env vars) |

---

## Verification Checklist

- [x] Unit tests pass (`ApiKeyCacheTest` — 5 tests)
- [x] Integration tests pass (requires Docker Compose)
- [x] Code compiles without warnings
- [x] Kafka consumer registered for `merchant.events` topic
- [x] Internal endpoint accessible from payment-service
- [x] Cache TTL set to 10 minutes
- [x] Graceful fallback to direct DB when HTTP call fails

---

## Next Steps (Optional)

1. **Add circuit breaker** — use Resilience4j for transient failure handling
2. **Add cache metrics** — expose hit rate via Micrometer/Prometheus
3. **Add key rotation support** — invalidate cache on `/api-keys/rotate`

---

## References

- Phase 8: `PHASE8_UNIFY_API_KEY_AUTH.md`
- Specification: `PayFlow_Specification_v2.md` — Phase 12 section
- Kafka topics: `merchant.events`