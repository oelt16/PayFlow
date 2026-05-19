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

> **Note**: Tests use TestContainers and require Docker to be running. On Windows with Docker Desktop + WSL2, you may need to enable TCP in Docker Desktop (`"hosts": ["tcp://0.0.0.0:2375"]`) or run tests from a Linux environment/WSL2.

### Option 2: Manual Verification via Running Services

Start PayFlow with Docker Compose, then verify the scheduler works by creating a test payment and checking the results.

#### Step 1: Start PayFlow

```bash
# From the infra directory
cd infra
docker compose up -d
```

#### Step 2: Create a Test PENDING Payment

The payment must be older than 1 hour AND have its `expires_at` timestamp in the past.

**Linux:**
```bash
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "
INSERT INTO payments.payments (id, merchant_id, amount, currency, status, description, metadata, client_secret, total_refunded, created_at, expires_at)
VALUES ('pay_test_expiry_001', 'mer_test_dev', 10000.00, 'USD', 'PENDING', 'Test expiry scheduler', '{}', 'test_secret_001', 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;
"
```

**Windows (PowerShell):**
```powershell
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "INSERT INTO payments.payments (id, merchant_id, amount, currency, status, description, metadata, client_secret, total_refunded, created_at, expires_at) VALUES ('pay_test_expiry_001', 'mer_test_dev', 10000.00, 'USD', 'PENDING', 'Test expiry scheduler', '{}', 'test_secret_001', 0, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '30 minutes') ON CONFLICT (id) DO NOTHING;"
```

> **Note**: The database is `payflow` (not `payments`), and the table is in the `payments` schema: `payments.payments`.

#### Step 3: Verify Payment is PENDING

```bash
# Linux / Windows
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT id, status, created_at, expires_at FROM payments.payments WHERE id = 'pay_test_expiry_001';"
```

Expected output:
```
         id          | status  |          created_at           |          expires_at           
---------------------+---------+-------------------------------+-------------------------------
 pay_test_expiry_001 | PENDING | ...-05-19 HH:MM:SS.ssssss+00 | ...-19 HH:MM:SS.ssssss+00
```

#### Step 4: Wait for Scheduler (5 minutes)

The scheduler runs every 5 minutes. You can:

**Option A: Wait for automatic run**
```bash
# Check the scheduler logs to see when it runs
docker logs payflow-payment-service-1 --tail 20 | grep -i expiry
```

**Option B: Manually trigger (if needed)**
The scheduler is automatic. Wait for the next run at minute :03, :08, :13, :18, :23, :28, :33, :38, :43, :48, :53, :58

#### Step 5: Verify Payment Status Changed to EXPIRED

```bash
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT id, status, created_at, expires_at FROM payments.payments WHERE id = 'pay_test_expiry_001';"
```

Expected output:
```
         id          | status  |          created_at           |          expires_at           
---------------------+---------+-------------------------------+-------------------------------
 pay_test_expiry_001 | EXPIRED | ...-05-19 HH:MM:SS.ssssss+00 | ...-19 HH:MM:SS.ssssss+00
```

#### Step 6: Verify Event Written to Outbox

```bash
docker exec payflow-postgres-1 psql -U payflow -d payflow -c "SELECT id, aggregate_id, event_type, created_at FROM payments.outbox_events WHERE event_type = 'payment.expired' ORDER BY created_at DESC LIMIT 5;"
```

Expected output:
```
                  id                  |             aggregate_id             |   event_type    |          created_at           
--------------------------------------+--------------------------------------+-----------------+-------------------------------
 xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx | pay_test_expiry_001              | payment.expired | 2026-05-19 HH:MM:SS.ssssss+00
```

### Option 3: Check Scheduler Logs

```bash
# View scheduler logs
docker logs payflow-payment-service-1 2>&1 | grep -i "PaymentExpiryScheduler"

# Or follow logs in real-time
docker logs payflow-payment-service-1 --follow | grep -i "expiry"
```

Expected log output:
```
2026-05-19T17:33:58.377Z  INFO 1 --- [payment-service] [   scheduling-1] c.p.p.i.s.PaymentExpiryScheduler         : Starting payment expiry job
2026-05-19T17:33:58.393Z  INFO 1 --- [payment-service] [   scheduling-1] c.p.p.i.s.PaymentExpiryScheduler         : Found 1 pending payments eligible for expiry
2026-05-19T17:33:58.401Z  INFO 1 --- [payment-service] [   scheduling-1] c.p.p.i.s.PaymentExpiryScheduler         : Expired payment: pay_test_expiry_001
2026-05-19T17:33:58.405Z  INFO 1 --- [payment-service] [   scheduling-1] c.p.p.i.s.PaymentExpiryScheduler         : Payment expiry job completed. Processed: 1/1
```

---

## SQL Queries for Verification

### Check for EXPIRED payments

```sql
SELECT id, status, created_at, expires_at
FROM payments.payments
WHERE status = 'EXPIRED'
ORDER BY created_at DESC
LIMIT 10;
```

### Check for PENDING payments that should be expired

```sql
SELECT id, merchant_id, amount, currency, created_at, expires_at
FROM payments.payments
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL '1 hour'
  AND expires_at <= NOW();
```

### Check outbox for PaymentExpiredEvent

```sql
SELECT id, aggregate_id, event_type, payload, created_at, published
FROM payments.outbox_events
WHERE event_type = 'payment.expired'
ORDER BY created_at DESC
LIMIT 10;
```

### Quick Status Check

```sql
-- Count payments by status
SELECT status, COUNT(*) as count
FROM payments.payments
GROUP BY status;
```

---

## Troubleshooting

### Payment not being expired?

1. **Check timestamps**: The payment must satisfy BOTH conditions:
   - `created_at < NOW() - INTERVAL '1 hour'` (created more than 1 hour ago)
   - `expires_at <= NOW()` (expires_at timestamp has passed)

2. **Check scheduler logs**:
   ```bash
   docker logs payflow-payment-service-1 2>&1 | grep -i "expiry\|error"
   ```

3. **Check if scheduler is running**:
   ```bash
   docker logs payflow-payment-service-1 2>&1 | grep "Starting payment expiry job"
   ```

### NullPointerException in scheduler?

If you see an error like:
```
java.lang.NullPointerException: Cannot invoke "Integer.intValue()" because the return value of "getCardExpMonth()" is null
```

This is caused by payments without card details. The fix is already applied in `PaymentPersistenceMapper.java` to handle null card data.

---

## Interview Talking Point

> "I process expired payments in batches of 100, each in its own transaction. A single failure doesn't roll back the whole batch. The expiry flows through the same outbox → Kafka → webhook pipeline as any other domain event — no special casing."

---

## Related Specification

See **Phase 13** in `PayFlow_Specification_v2.md` for complete requirements.