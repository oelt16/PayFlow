# Proposal: Phase 11 — Idempotency Keys

> **Source**: Engram observation #13

## Intent

Payment APIs are inherently retry-prone — network timeouts, client SDK retries, and webhook delivery retries all send the same request multiple times. Without idempotency, a client retry becomes a duplicate charge.

## Scope

- Flyway migration `V3__add_idempotency_keys.sql`
- `IdempotencyFilter` — servlet filter for `POST /v1/payments`, `/capture`, `/refunds`
- `IdempotencyKey` JPA entity + `IdempotencyKeyRepository`
- `IdempotencyService` — hash computation, lookup, storage
- `IdempotencyPurgeScheduler` — daily 2am purge
- Unit + integration tests

### Out of Scope
- Idempotency for GET/DELETE
- Custom TTL configuration (24h hardcoded)

## Approach

**Filter-based, pre-auth chain.** `IdempotencyFilter` runs after `ApiKeyAuthenticationFilter`. Key flow:
1. Extract `Idempotency-Key` header
2. Compute SHA-256 of request body
3. Lookup by `(merchant_id, key)`
   - Match + hash match → cached response (200), short-circuit
   - Match + hash mismatch → 422 `idempotency_key_reuse`
   - No match → proceed, store result atomically
4. Daily purge deletes expired rows (24h TTL)
