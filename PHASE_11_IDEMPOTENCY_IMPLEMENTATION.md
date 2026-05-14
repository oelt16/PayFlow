# Phase 11 — Idempotency Keys Implementation

## Overview

**Status**: ✅ Implemented and Verified  
**Date**: 2026-05-14  
**Feature**: Client-supplied idempotency keys for POST endpoints

---

## What Was Implemented

### Why Idempotency Keys?

Payment APIs are inherently retry-prone:
- Network timeouts cause client SDK retries
- Webhook delivery retries send the same request
- Users accidentally double-click submit buttons

Without idempotency, a client retry creates **duplicate charges**. This is a fundamental production primitive used by Stripe, PayPal, Adyen, and every serious fintech API.

### What Was Built

| Component | File | Description |
|-----------|------|-------------|
| Database table | `V3__add_idempotency_keys.sql` | Stores idempotency key + request hash + cached response |
| JPA Entity | `IdempotencyKeyJpaEntity.java` | Entity mapping for idempotency_keys table |
| Repository | `IdempotencyKeySpringDataRepository.java` | Spring Data repository for lookups |
| Service | `IdempotencyService.java` | Computes SHA-256 hash, lookup, store logic |
| Filter | `IdempotencyFilter.java` | Servlet filter that handles Idempotency-Key header |
| Config | `IdempotencyConfig.java` | 24-hour TTL configuration |
| Exception | `IdempotencyKeyReuseException.java` | Thrown when key is reused with different body |

### How It Works

1. **Client sends** `Idempotency-Key: <uuid>` header with POST request
2. **Filter extracts** the header and computes SHA-256 of request body
3. **Lookup in DB** by `(merchant_id, idempotency_key)`:
   - **Key exists + hash matches** → Return cached response (same paymentId)
   - **Key exists + hash differs** → Return 422 `idempotency_key_reuse`
   - **Key doesn't exist** → Proceed, store result after controller executes
4. **TTL**: Keys expire after 24 hours
5. **Purge**: Daily scheduler at 2am UTC deletes expired keys

### Endpoints Supporting Idempotency

- `POST /v1/payments` — Create payment
- `POST /v1/payments/:id/capture` — Capture payment
- `POST /v1/payments/:id/refunds` — Issue refund

---

## Verification

### Test with Bruno

Import the collection from `bruno/payflow/payflow/`:

| Request | Test Case | Expected Result |
|---------|-----------|-----------------|
| #2 | First payment with key | 201 + new paymentId |
| #3 | Retry with same key | 200 + **same** paymentId |
| #4 | Same key, different body | 422 `idempotency_key_reuse` |
| #5 | No key (normal flow) | 201 + new paymentId |

### Test with Curl

**First request:**
```bash
curl -X POST http://localhost:3000/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Idempotency-Key: test-key-123" \
  -d '{"amount": 5000, "currency": "USD", "description": "Test"}'
```

**Retry with same key:**
```bash
# Same request - should return SAME paymentId
curl -X POST http://localhost:3000/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Idempotency-Key: test-key-123" \
  -d '{"amount": 5000, "currency": "USD", "description": "Test"}'
```

**Different body with same key (should fail):**
```bash
# Same key but DIFFERENT amount - should return 422
curl -X POST http://localhost:3000/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Idempotency-Key: test-key-123" \
  -d '{"amount": 9999, "currency": "EUR", "description": "Different"}'
```

### Verify in Database

**Linux / macOS:**
```bash
# See all idempotency keys stored
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT key, merchant_id, http_status, created_at, expires_at FROM payments.idempotency_keys ORDER BY created_at DESC;"

# See payments created (should only be ONE for the same idempotency key)
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT id, amount, currency, created_at FROM payments.payments WHERE description = 'Test' ORDER BY created_at;"

# Count payments for idempotency test (should be 1, not 2!)
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT COUNT(*) FROM payments.payments WHERE description = 'Test';"
```

**Windows (PowerShell):**
```powershell
# See all idempotency keys stored
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT key, merchant_id, http_status, created_at, expires_at FROM payments.idempotency_keys ORDER BY created_at DESC;"

# See payments created (should only be ONE for the same idempotency key)
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT id, amount, currency, created_at FROM payments.payments WHERE description = 'Test' ORDER BY created_at;"

# Count payments for idempotency test (should be 1, not 2!)
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT COUNT(*) FROM payments.payments WHERE description = 'Test';"
```

### Verify in Docker Logs

**Linux / macOS:**
```bash
# Check for idempotency storage (appears after first request)
docker logs payflow-payment-service-1 2>&1 | grep "Stored idempotency key"

# Check for cache hits (appears on retry)
docker logs payflow-payment-service-1 2>&1 | grep -i "cache hit"

# Check for 422 errors (key reuse)
docker logs payflow-payment-service-1 2>&1 | grep "idempotency_key_reuse"

# Full idempotency log trace
docker logs payflow-payment-service-1 2>&1 | grep -i idempotency
```

**Windows (PowerShell):**
```powershell
# Check for idempotency storage (appears after first request)
docker logs payflow-payment-service-1 2>&1 | Select-String -Pattern "Stored idempotency key"

# Check for cache hits (appears on retry)
docker logs payflow-payment-service-1 2>&1 | Select-String -Pattern "Cache hit"

# Check for 422 errors (key reuse)
docker logs payflow-payment-service-1 2>&1 | Select-String -Pattern "idempotency_key_reuse"

# Full idempotency log trace
docker logs payflow-payment-service-1 2>&1 | Select-String -Pattern "idempotency"
```

---

## Key Files Modified/Created

```
backend/payment-service/
├── src/main/resources/db/migration/
│   └── V3__add_idempotency_keys.sql          # NEW - DB table
├── src/main/java/com/payflow/payment/
│   ├── application/
│   │   ├── IdempotencyService.java           # NEW - Core service
│   │   ├── IdempotencyResult.java             # NEW - Result record
│   │   ├── config/
│   │   │   └── IdempotencyConfig.java         # NEW - TTL config
│   │   └── exception/
│   │       └── IdempotencyKeyReuseException.java  # NEW - 422 exception
│   ├── api/filter/
│   │   └── IdempotencyFilter.java            # MODIFIED - Added response wrapper
│   └── infrastructure/persistence/jpa/
│       ├── IdempotencyKeyJpaEntity.java      # NEW - JPA entity
│       └── IdempotencyKeySpringDataRepository.java  # NEW - Repository
```

---

## Interview Talking Points

> "The filter stores the SHA-256 of the request body alongside the key. If the same key is replayed with a different body, we return 422 — this prevents a subtle class of bugs where a client accidentally reuses a key for a different operation."

> "We use ContentCachingResponseWrapper to capture the controller's response and store it in the idempotency_keys table. On cache hit, we return the cached response verbatim — same paymentId, same status code."

> "The 24-hour TTL is a balance between allowing long retry windows (network issues can take hours to resolve) and keeping storage bounded. The daily purge job at 2am UTC keeps the table clean."

---

## Troubleshooting

### Idempotency Not Working?

**Linux / macOS:**
```bash
# 1. Check service is running
docker ps --filter "name=payment-service"

# 2. Check logs
docker logs payflow-payment-service-1

# 3. Verify DB table exists
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "\dt payments.*"

# 4. Check idempotency_keys table has data
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT COUNT(*) FROM payments.idempotency_keys;"
```

**Windows (PowerShell):**
```powershell
# 1. Check service is running
docker ps --filter "name=payment-service"

# 2. Check logs
docker logs payflow-payment-service-1

# 3. Verify DB table exists
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "\dt payments.*"

# 4. Check idempotency_keys table has data
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT COUNT(*) FROM payments.idempotency_keys;"
```

### Rebuild After Code Changes

**Linux / macOS:**
```bash
docker compose -f infra/docker-compose.yml build payment-service
docker compose -f infra/docker-compose.yml up -d
```

**Windows (PowerShell):**
```powershell
docker compose -f infra/docker-compose.yml build payment-service
docker compose -f infra/docker-compose.yml up -d
```

---

## Related Specification

See [PayFlow_Specification_v2.md](./PayFlow_Specification_v2.md) Section **Phase 11 — Idempotency Keys** (lines 497-524).