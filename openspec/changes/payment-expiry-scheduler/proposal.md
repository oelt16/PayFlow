# Proposal: Payment Expiry Scheduler (Phase 13)

> **Source**: Engram observation #25

## Intent

Complete the Payment Expiry domain feature by implementing the scheduler that automatically expires stale pending payments. Closes the loop on `Payment.expire()` from Phase 1 — without this scheduler, pending payments never transition to EXPIRED.

## Scope

- Repository query: `findPendingOlderThan(cutoff, limit)`
- Outbox mapping for `PaymentExpiredEvent`
- `PaymentExpiryScheduler` — `@Scheduled(fixedRate = 60_000)`
- Integration test with Testcontainers

### Out of Scope
- Manual payment expiration API
- Expiring payments with explicit `expires_at`

## Approach

**Batch processing with per-item transactions:**
1. Query: `PENDING + created_at < (now - 1h) + expires_at IS NULL` (batch: 100)
2. Each payment processed in own `@Transactional`
3. `payment.expire()` → `PaymentExpiredEvent` → outbox
4. Failures logged and skipped (continue with next payment)
