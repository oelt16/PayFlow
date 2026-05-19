# Phase 13 — Payment Expiry Scheduler

## Why This Phase?

The PayFlow domain model has had an `expire()` method on the Payment aggregate since Phase 1, along with a `PaymentExpiredEvent` event type. However, the **scheduler that actually calls this method was never implemented**. Without it, PENDING payments accumulate indefinitely, which would be a clear gap to any technical reviewer evaluating the project.

This phase completes the domain feature by implementing a scheduled job that automatically expires stale PENDING payments.

## What Was Implemented

### Components

| Component | File | Description |
|-----------|------|-------------|
| **Scheduler** | `infrastructure/scheduler/PaymentExpiryScheduler.java` | `@Scheduled(fixedRate = 300000)` — runs every 5 minutes |
| **Repository Query** | `PaymentSpringDataRepository.java` | Query: `status=PENDING AND createdAt < (now-1h) AND expiresAt <= now` |
| **Outbox Mapping** | `OutboxEventPayloadMapper.java` | Maps `PaymentExpiredEvent` to outbox table |
| **Integration Test** | `PaymentExpirySchedulerTest.java` | 3 test cases covering happy path and edge cases |

### Implementation Details

- **Batch size**: 100 payments per run
- **Transaction strategy**: Each payment expires in its own transaction — a single failure doesn't rollback the batch
- **Event flow**: Same as capture/cancel — `PaymentExpiredEvent` → outbox table → Kafka → notification-service → webhook
- **Clock injection**: Uses application `Clock` bean for testability

### Files Changed

```
payment-service/src/main/java/com/payflow/payment/
├── application/port/PaymentRepository.java                    # Added findPendingOlderThan
├── infrastructure/persistence/jpa/
│   ├── PaymentSpringDataRepository.java                       # Added query method
│   └── JpaPaymentRepositoryAdapter.java                       # Implemented repository method
├── infrastructure/outbox/OutboxEventPayloadMapper.java       # Added PaymentExpiredEvent mapping
└── infrastructure/scheduler/PaymentExpiryScheduler.java     # NEW

payment-service/src/test/java/com/payflow/payment/
└── integration/PaymentExpirySchedulerTest.java               # NEW
```

---

## How to Test / Verify

### Option 1: Run Integration Tests

**Linux / macOS:**
```bash
cd backend
./mvnw verify -pl payment-service -Dtest=PaymentExpirySchedulerTest
```

**Windows (PowerShell):**
```powershell
cd backend
./mvnw verify -pl payment-service -Dtest=PaymentExpirySchedulerTest
```

> **Note**: Tests use TestContainers and require Docker to be running and accessible.

### Option 2: Manual Verification via Database

Start PayFlow with Docker Compose, then verify the scheduler works by checking the database:

#### Linux

```bash
# 1. Start PayFlow
cd infra
docker compose up -d

# 2. Create a PENDING payment older than 1 hour
docker exec -i payflow-postgres-1 psql -U payflow -d payments < create_old_pending_payment.sql

# 3. Wait for scheduler to run (5 minutes) or trigger manually:
docker exec payflow-payment-service-1 java -cp /app/app.jar com.payflow.payment.PaymentServiceApplication

# 4. Check payment status
docker exec -i payflow-postgres-1 psql -U payflow -d payments -c "SELECT id, status, created_at FROM payments;"
```

#### Windows (PowerShell)

```powershell
# 1. Start PayFlow
cd infra
docker compose up -d

# 2. Create a PENDING payment older than 1 hour
docker exec payflow-postgres-1 psql -U payflow -d payments -c "INSERT INTO payments (id, merchant_id, amount, currency, status, created_at, expires_at) VALUES ('pay_test_123', 'mer_abc', 10000, 'USD', 'PENDING', NOW() - INTERVAL '2 hours', NOW() + INTERVAL '1 hour');"

# 3. Wait 5 minutes or check logs for scheduler activity
docker logs payflow-payment-service-1 --follow

# 4. Query payment status
docker exec payflow-postgres-1 psql -U payflow -d payments -c "SELECT id, status, created_at FROM payments WHERE id = 'pay_test_123';"
```

### SQL Queries for Verification

**Check for EXPIRED payments:**
```sql
-- PostgreSQL
SELECT id, status, created_at, expires_at
FROM payments
WHERE status = 'EXPIRED';
```

**Check for PENDING payments that should be expired:**
```sql
-- PostgreSQL: Find pending payments older than 1 hour
SELECT id, merchant_id, amount, currency, created_at, expires_at
FROM payments
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL '1 hour'
  AND expires_at <= NOW();
```

**Check outbox for PaymentExpiredEvent:**
```sql
-- PostgreSQL
SELECT id, aggregate_id, event_type, payload, created_at, published
FROM outbox_events
WHERE event_type = 'payment.expired'
ORDER BY created_at DESC
LIMIT 10;
```

---

## Interview Talking Point

> "I process expired payments in batches of 100, each in its own transaction. A single failure doesn't roll back the whole batch. The expiry flows through the same outbox → Kafka → webhook pipeline as any other domain event — no special casing."

---

## Related Specification

See **Phase 13** in `PayFlow_Specification_v2.md` for complete requirements.