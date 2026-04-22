# Phase 8: unify API key authentication

## What went wrong

After merchant registration from the dashboard, **Settings** looked fine, but **Overview**, **Payments**, and **Webhooks** failed with **401 Unauthorized** on `GET /api/v1/payments` and `GET /api/v1/webhooks`.

The stack had **two different ways** to decide whether a Bearer API key was valid:

1. **merchant-service** stored each merchant’s key as a **BCrypt hash** in PostgreSQL (`merchants.merchants`), looked up rows by the first **8 characters** of the raw key (`key_prefix`), then verified the full key with BCrypt. Registration returned a new random `sk_test_…` key; that flow was correct.

2. **payment-service** and **webhook-service** used a **static allowlist** in `application.yml` (`payflow.security.api-keys`): exact string match of the full key to a hardcoded `merchant-id` (for example `sk_test_dev` → `mer_test_dev`).

So the key the UI saved after registration **never appeared** in the YAML maps on payment or webhook. Those services correctly answered **401** for “unknown key.”

If someone then pasted **`sk_test_dev`** to match the YAML, auth could succeed on payment/webhook, but **business logic** still assumed a merchant that existed in the shared model; mismatched or missing merchant rows led to confusing follow-on errors (for example **400** on payment list paths when the resolved id did not line up with real data).

## What we changed

We made **payment-service** and **webhook-service** use the **same source of truth** as merchant-service: read-only access to **`merchants.merchants`**, with the **same algorithm** (prefix lookup + BCrypt match on `key_hash`).

Concretely:

- Added **`JdbcApiKeyAuthenticator`** in each service: query by `key_prefix`, then `BCryptPasswordEncoder.matches(rawKey, key_hash)` until one row matches; set **`MerchantContext`** from the row’s **`id`**.
- **Rewrote `ApiKeyAuthenticationFilter`** to depend on that component instead of an in-memory map from config.
- **Removed** `PayflowSecurityProperties` and **`payflow.security.api-keys`** from YAML.
- Added **`spring-security-crypto`** for BCrypt on those two services.
- Ensured the **`merchants` schema exists** when those services start (Hikari **`connection-init-sql`**: `CREATE SCHEMA IF NOT EXISTS merchants`) so a cold payment or webhook JVM does not fail before merchant-service Flyway has run; the **table and rows** still come from merchant-service (writes stay there).

**merchant-service** was already correct; no behavioural change required there beyond remaining the only writer to merchant key fields.

## Tests and local ergonomics

- **payment-service** integration tests seed a **`merchants.merchants`** row for `mer_test_dev` with a BCrypt hash of **`sk_test_dev`**, so existing tests that send `Authorization: Bearer sk_test_dev` keep working without YAML.
- **webhook-service** integration tests do the same for their Postgres container.
- **`PaymentsControllerWebMvcTest`** uses **`@MockBean JdbcApiKeyAuthenticator`** so `@WebMvcTest` still loads **`ApiKeyAuthenticationFilter`** without a full database.

## Frontend

No change was required to how keys are sent (**Bearer** from the persisted store). Copy on **Settings** was updated so it no longer refers to “static dev keys” on payment or webhook; those services now follow the same DB-backed rules as the merchant API.

## Result

One registered key works for **merchant**, **payment**, and **webhook** APIs as long as all services point at the **same PostgreSQL** (for example the Compose stack from phase 7). Cross-service API key drift from duplicated YAML config is removed for this codebase path.
