# Delta Specification: Idempotency Keys

> **Source**: Engram observation #14

## ADDED Requirements

### Requirement: Idempotency Key Header Processing
- System MUST validate/extract `Idempotency-Key` header from `POST /v1/payments`, `/capture`, `/refunds`
- Header value: UUID string, max 64 chars
- Absent header → pass-through

### Requirement: Request Hash Computation
- SHA-256 hash of complete request body
- 64-character lowercase hex representation
- Body MUST be buffered before hashing to enable downstream access

### Requirement: Cache Lookup
- Query `idempotency_keys` by composite key `(merchant_id, idempotency_key)`
- Match + hash match → return cached response + `X-Idempotent-Replayed: true`
- Expired keys (expires_at < NOW()) treated as non-existent

### Requirement: Body Mismatch Detection
- Same key + different body hash → HTTP 422 `idempotency_key_reuse`

### Requirement: New Key Storage
- Store: key, merchant_id, request_hash, response_body (JSONB), http_status, created_at, expires_at (+24h)

### Requirement: Daily Purge Scheduler
- `@Scheduled(cron = "0 0 2 * * *")` — delete rows where `expires_at < NOW()`

## Data Model

| Field | Type | Constraints |
|-------|------|-------------|
| key | VARCHAR(64) | PRIMARY KEY |
| merchant_id | VARCHAR(36) | NOT NULL |
| request_hash | VARCHAR(64) | NOT NULL (SHA-256 hex) |
| response_body | JSONB | NOT NULL |
| http_status | INT | NOT NULL |
| created_at | TIMESTAMPTZ | DEFAULT NOW() |
| expires_at | TIMESTAMPTZ | NOT NULL (created_at + 24h) |

Index: `(merchant_id, expires_at)`
