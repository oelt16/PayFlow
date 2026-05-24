# Specification: Cross-Service API Key Cache

> **Source**: Engram observation #21

## Requirements

### R1: Caffeine Cache
- payment-service and webhook-service MUST implement Caffeine cache for API key validation
- 10-minute TTL, max-size 10,000
- Cache key: API key prefix (e.g., "pk_live_abc123")
- Cache entry: merchantId, keyHash, isActive, cachedAt

### R2: Cache Hit
- Key in cache → return cached ValidatedMerchant immediately
- NO HTTP call to merchant-service
- NO database query

### R3: Cache Miss + Service Available
- Key not in cache → call `POST /v1/internal/merchants/validate-key`
- Store response in cache
- Return validated merchant to filter chain

### R4: Cache Miss + Service Down
- Key not in cache, merchant-service unavailable → 503 Service Unavailable
- No stale data served

### R5: Internal Validation Endpoint
- `POST /v1/internal/merchants/validate-key` on merchant-service
- Takes `{"key_prefix": "pk_live_abc"}` → returns `{merchantId, keyHash, isActive}`
- HTTP 404 if key not found

### R6: Kafka Invalidation
- Consumer on `merchant.events` topic
- On `MerchantDeactivatedEvent` → evict cache entry immediately
- Retry with exponential backoff on failure
- Dead Letter Queue after max retries
