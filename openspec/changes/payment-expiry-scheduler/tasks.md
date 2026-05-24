# Tasks: Payment Expiry Scheduler

> **Source**: Engram observation #28

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~150 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |

## Phase 1: Repository Layer

- [ ] 1.1 Add `findPendingOlderThan(cutoff, expiresAt, limit)` to PaymentRepository interface
- [ ] 1.2 Add Spring Data query `findByStatusAndCreatedAtBeforeAndExpiresAtBefore` in PaymentSpringDataRepository
- [ ] 1.3 Implement in JpaPaymentRepositoryAdapter

## Phase 2: Outbox Integration

- [ ] 2.1 Add PaymentExpiredEvent mapping to OutboxEventPayloadMapper

## Phase 3: Scheduler Implementation

- [ ] 3.1 Create PaymentExpiryScheduler — `@Scheduled(fixedRate = 300000)` (5 min)
- [ ] 3.2 Implement `expireOldPendingPayments()` with batch processing
- [ ] 3.3 Add per-item try-catch for failure isolation

## Phase 4: Testing

- [ ] 4.1 Integration test: seed 2hr-old PENDING, verify EXPIRED
- [ ] 4.2 Verify PaymentExpiredEvent in outbox
- [ ] 4.3 Verify 30min-old PENDING remains unchanged
