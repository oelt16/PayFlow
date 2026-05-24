# Design: Cross-Service API Key Cache

> **Source**: Engram observation #22

## Technical Approach

Caffeine local cache in payment-service and webhook-service with read-through pattern. Cache miss calls merchant-service internal endpoint. Kafka consumer evicts on merchant deactivation.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Cache location | Inject into `JdbcApiKeyAuthenticator` | Keeps filter unchanged, single responsibility |
| Cache key | `key_prefix` | Matches existing `resolveMerchantId()` behavior |
| TTL | 10 min (600s) | Balance between hit rate and staleness |
| Event change | Extend `MerchantDeactivatedEvent` with `keyPrefix` | Eviction needs the exact key_prefix |

## Data Flow

```
Request → ApiKeyAuthenticationFilter
       → JdbcApiKeyAuthenticator.resolveMerchantId()
           ├─ Cache hit → return MerchantId
           └─ Cache miss → POST /internal/validate-key → cache → return MerchantId

Kafka: MerchantDeactivatedEvent → MerchantEventConsumer → ApiKeyCache.evict()
```

## File Changes

| File | Action | Service |
|------|--------|---------|
| `pom.xml` | Modify (add caffeine) | payment-service + webhook-service |
| `ApiKeyCache.java` | Create | payment-service + webhook-service |
| `JdbcApiKeyAuthenticator.java` | Modify (inject cache) | payment-service + webhook-service |
| `MerchantEventConsumer.java` | Create | payment-service + webhook-service |
| `ValidateKeyController.java` | Create | merchant-service |
| `MerchantDeactivatedEvent.java` | Modify (add keyPrefix) | merchant-service |
| `MerchantEventPayloadMapper.java` | Modify | merchant-service |
