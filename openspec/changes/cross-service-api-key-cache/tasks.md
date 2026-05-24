# Tasks: Cross-Service API Key Cache

> **Source**: Engram observation #23

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350–450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Status | 🟡 Planned — not yet implemented |

## Phase 1: Infrastructure

- [ ] 1.1 Add caffeine dependency to payment-service `pom.xml`
- [ ] 1.2 Add caffeine dependency to webhook-service `pom.xml`
- [ ] 1.3 Add `payflow.api-key-cache` config to both `application.yml`

## Phase 2: Core Cache

- [ ] 2.1 Create `ValidatedMerchant` record in payment-service
- [ ] 2.2 TDD: `ApiKeyCacheTest` + implementation in payment-service
- [ ] 2.3 Create `ValidatedMerchant` record + cache in webhook-service

## Phase 3: Merchant Service Changes

- [ ] 3.1 Extend `MerchantDeactivatedEvent` with `keyPrefix`
- [ ] 3.2 Update `MerchantEventPayloadMapper`
- [ ] 3.3 TDD: `ValidateKeyController` — `POST /v1/internal/merchants/validate-key`

## Phase 4: Kafka Consumer

- [ ] 4.1 TDD: `MerchantEventConsumer` in payment-service
- [ ] 4.2 TDD: `MerchantEventConsumer` in webhook-service

## Phase 5: Cache Integration

- [ ] 5.1 Modify `JdbcApiKeyAuthenticator` in payment-service (cache-first)
- [ ] 5.2 Modify `JdbcApiKeyAuthenticator` in webhook-service (cache-first)

## Phase 6: Integration Tests

- [ ] 6.1 Integration test: cache hit returns immediately
- [ ] 6.2 Integration test: cache miss calls merchant-service
- [ ] 6.3 Integration test: service down on miss returns 503
- [ ] 6.4 Integration test: Kafka event evicts cached entry
