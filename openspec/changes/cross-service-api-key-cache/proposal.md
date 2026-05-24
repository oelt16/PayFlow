# Proposal: Cross-Service API Key Cache

> **Source**: Engram observation #20

## Intent

Eliminate per-request DB round-trips in payment-service and webhook-service for API key validation. Every authenticated request hits the `merchants` table to fetch and BCrypt-compare the key hash — a mandatory DB query. Introduce local Caffeine caching with Kafka-based invalidation.

## Scope

- Caffeine cache in payment-service + webhook-service
- Internal `/internal/validate-key` endpoint on merchant-service
- Kafka consumer for `MerchantDeactivatedEvent` in both services
- Read-through cache pattern with cache-aside on miss

## Approach

| Component | Detail |
|-----------|--------|
| Cache key | `key_prefix` (e.g., "pk_live_abc") |
| TTL | 10 minutes |
| Max size | 10,000 entries |
| Cache miss | HTTP call to merchant-service `/internal/validate-key` |
| Invalidation | Kafka `merchant.events` → evict on `MerchantDeactivatedEvent` |
| Fallback | Service down → 503 (fail closed) |
