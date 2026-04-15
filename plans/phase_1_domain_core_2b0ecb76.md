---
name: Phase 1 domain core
overview: "Phase 1 per the spec is the payments bounded context as pure Java: Payment aggregate (full state machine), value objects, domain events, and domain-only unit tests with strong coverage. The repo already implements most of this in [backend/payment-service](backend/payment-service); the plan finishes spec alignment, fills test gaps, and optionally enforces coverage with JaCoCo."
todos:
  - id: vo-tests
    content: Add TDD tests for MerchantId, PaymentId, RefundId, CardDetails (and CardBrand if needed)
    status: completed
  - id: money-edge-tests
    content: "Extend MoneyTest: invalid non-XXX currency; subtract/add edge cases per spec spirit"
    status: completed
  - id: payment-edge-tests
    content: "Extend PaymentTest: illegal transitions, multi-step partial refunds, pullDomainEvents, currency mismatch on refund"
    status: completed
  - id: jacoco-optional
    content: "Optional: JaCoCo on payment-service scoped to com.payflow.payment.domain with coverage check in verify"
    status: completed
isProject: false
---

# Phase 1 — domain core (payments)

## What the spec requires

From [PayFlow_Specification.docx.txt](PayFlow_Specification.docx.txt) §11:

- **Payment aggregate** with the state machine in §3.1 (create → PENDING; capture → CAPTURED; cancel → CANCELLED; refund → REFUNDED / PARTIAL_REFUND; expire → EXPIRED), emitting the listed domain events.
- **Value objects**: Money, PaymentId (`PaymentId.generate()`), MerchantId, CardDetails (last4, brand, expiry; no PAN), PaymentStatus enum.
- **TDD / tests**: §7.3 lists concrete scenarios for Payment and Money; target **100% unit test coverage on the domain layer**; **no Spring, no DB**.

Kafka envelope shape (§5.2) and REST DTOs are **out of scope** for Phase 1; domain events should carry the fields needed to map to §5.3 payloads later (current records in `domain/event/` are already close).

## Current state in the repo

`[Payment.java](backend/payment-service/src/main/java/com/payflow/payment/domain/Payment.java)` already implements the full lifecycle, pending TTL default of 1h, cumulative refunds with `RefundId` on each refund, and event recording. `[Money.java](backend/payment-service/src/main/java/com/payflow/payment/domain/Money.java)` validates non-negative amounts and ISO currencies (via `Currency.getInstance`). Value objects and exceptions exist under `[com.payflow.payment.domain](backend/payment-service/src/main/java/com/payflow/payment/domain/)`.

`[PaymentTest](backend/payment-service/src/test/java/com/payflow/payment/domain/PaymentTest.java)` and `[MoneyTest](backend/payment-service/src/test/java/com/payflow/payment/domain/MoneyTest.java)` cover the §7.3 examples that apply at aggregate/VO level.

**Gaps vs “100% domain coverage” and robustness:**


| Area          | Gap                                                                                                                                                                                                                                |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Value objects | No dedicated tests for `MerchantId` (blank/null), `PaymentId` / `RefundId` factories, `CardDetails` invariants (bad last4, month, year).                                                                                           |
| Money         | §7.3 “invalid currency” is only exercised with `XXX`; add at least one invalid non-ISO code; optional tests for subtract-underflow and cross-currency `add`/`subtract`.                                                            |
| Payment       | Missing negative paths: refund while PENDING or CANCELLED; cancel/capture when not PENDING; second full refund after REFUNDED; `pullDomainEvents()` clears buffer; multiple partial refunds in sequence; refund currency mismatch. |
| Build         | Parent `[backend/pom.xml](backend/pom.xml)` has no JaCoCo; “100% coverage” is not enforced in CI yet.                                                                                                                              |


## Implementation approach (TDD)

1. **Add a failing test** for each gap above, then minimal production changes only if tests expose bugs (avoid scope creep).
2. **Optional but recommended:** add JaCoCo to the **payment-service** module (report + `check` rule on `domain` package) so `./mvnw -pl payment-service verify` fails if domain coverage drops. Keep the rule scoped to `com.payflow.payment.domain` so future application/infrastructure code in Phase 2 does not block Phase 1 metrics.
3. **Spec nuance — amounts:** API examples use integer minor units (e.g. `10000`); the domain uses `BigDecimal` with scale 2, which matches §6.1 `NUMERIC(19,2)`. Document mentally (or in a one-line comment in tests) that **API adapters** will convert minor → major in Phase 2; no domain change required for Phase 1 unless you explicitly want Money in minor units end-to-end.

## Explicit non-goals (defer to later phases)

- Spring Boot, JPA, Flyway, REST, outbox, Kafka, Docker, other services (`merchant-service`, `webhook-service`, `notification-service` stay stubs).

## Verification

- `cd backend && ./mvnw -pl payment-service test` (and `verify` if JaCoCo is added) with **zero** Spring context loaded for domain tests.

## Optional follow-up (not Phase 1)

- Align domain event type strings with §5.3 `eventType` values when you add the outbox/Kafka layer (Phase 3).

