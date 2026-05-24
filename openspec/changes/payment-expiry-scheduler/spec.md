# Specification: Payment Expiry Scheduler

> **Source**: Engram observation #26

## Requirements

### R1: Scheduled Expiry Check
- Runs every 60 seconds (adjusted to 5 minutes in implementation)
- Query: `status=PENDING AND created_at < (NOW() - 1h) AND expires_at IS NULL`
- Batch size: 100

### R2: Per-Item Transaction Isolation
- Each payment expiration in its own transaction
- Failure in one payment does NOT affect others
- Errors logged with payment ID, processing continues

### R3: PaymentExpiredEvent Emission
- Each expired payment writes `PaymentExpiredEvent` to outbox
- Payload: payment_id, merchant_id, expired_at

### R4: Clock-Based Time Source
- Uses application `Clock` bean (enables deterministic testing)

## Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC-1 | Scheduler runs periodically |
| AC-2 | Only PENDING + created_at > 1h ago are expired |
| AC-3 | Per-item transaction isolation |
| AC-4 | PaymentExpiredEvent in outbox for each expired payment |
| AC-5 | Integration test: seed 2hr-old PENDING, trigger, assert EXPIRED |
