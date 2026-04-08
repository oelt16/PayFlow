# notification-service domain model

This bounded context does **not** define a payment aggregate. It **consumes** events published by `payment-service` on `payments.events`. The only stable “model” in code is the **wire envelope** (`PaymentEventEnvelope`), which mirrors the producer’s JSON contract. No `domain` package exists yet; richer notification rules (templates, channels, idempotency) would live there in later phases.

```mermaid
classDiagram
  direction TB

  class PaymentEventEnvelope <<integration contract>> {
    +eventId String
    +eventType String
    +aggregateId String
    +merchantId String
    +occurredAt Instant
    +payload Map~String,Object~
  }

  note for PaymentEventEnvelope "JSON from Kafka; partition key is merchantId at producer.\nSame shape as payment-service PaymentEventEnvelope."
```

## Event types (consumer expectations)

The unit test [`PaymentEventConsumerTest`](../src/test/java/com/payflow/notification/consumer/PaymentEventConsumerTest.java) accepts these `eventType` values as valid envelope payloads:

| `eventType` | Meaning (from payments BC) |
| --- | --- |
| `payment.created` | Payment persisted, pending capture |
| `payment.captured` | Capture completed |
| `payment.cancelled` | Cancelled before capture |
| `payment.refunded` | Refund recorded |
| `payment.expired` | Pending payment expired |

Malformed JSON is logged at WARN and does not propagate; there is no dead-letter queue in this phase.

## Relationship to payment-service

- **Producer:** `payment-service` writes rows to the outbox and `OutboxRelay` publishes JSON matching this envelope.
- **Consumer:** `notification-service` maps bytes to `PaymentEventEnvelope` and logs; it does not mutate payment state.

For the full payment aggregate, value objects, and domain events, see [`payment-service/docs/domain-model.md`](../../payment-service/docs/domain-model.md).
