# Design: Payment Expiry Scheduler

> **Source**: Engram observation #27

## Technical Approach

Scheduled batch job every 5 minutes to expire stale PENDING payments older than 1 hour. Each payment in its own transaction for failure isolation.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Query strategy | `status=PENDING AND createdAt<cutoff AND expiresAt<=now` | expiresAt always set on creation |
| Transaction isolation | No `@Transactional` on scheduler, auto-commit per save | Simplest, failure in one doesn't affect others |
| Clock injection | Application `Clock` bean | Deterministic testing |

## Data Flow

```
Scheduler (every 5 min)
    → Calculate cutoff = now - 1h
    → Repository.findPendingOlderThan(cutoff, 100)
    → For each Payment:
        ├─ payment.expire(now) → status=EXPIRED
        ├─ repository.update(payment)
        └─ Outbox: PaymentExpiredEvent
```

## File Changes

| File | Action |
|------|--------|
| `PaymentRepository.java` | Modify: add `findPendingOlderThan` |
| `PaymentSpringDataRepository.java` | Modify: add Spring Data query |
| `JpaPaymentRepositoryAdapter.java` | Modify: implement method |
| `OutboxEventPayloadMapper.java` | Modify: add PaymentExpiredEvent mapping |
| `PaymentExpiryScheduler.java` | Create |
| `PaymentExpirySchedulerTest.java` | Create |

## Testing Strategy

| Layer | Approach |
|-------|----------|
| Integration | `@DataJpaTest`, seed 2hr-old + 30min-old payments, call scheduler, assert EXPIRED |
| Integration | Per-item isolation: inject failure on 2nd payment, verify 1st and 3rd expire |
| Integration | Outbox table has event after scheduler run |
