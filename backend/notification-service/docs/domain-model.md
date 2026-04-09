# notification-service domain model

This bounded context does **not** own the payment aggregate. It **consumes** JSON envelopes from Kafka topic `payments.events` (same contract as `payment-service` publishes via the outbox relay). The stable in-code contract is **`PaymentEventEnvelope`**.

After a successful parse, the service **forwards** the event to **webhook-service** for endpoint matching and delivery (HTTP from webhook-service to merchant URLs). That forwarding is not part of the payment domain; it is an integration concern implemented in `WebhookDispatchClient`.

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

  note for PaymentEventEnvelope "JSON from Kafka; producer uses merchantId as message key.\nSame conceptual shape as payment-service outbox/Kafka payload envelope."
```

## Event types (consumer expectations)

The unit test [`PaymentEventConsumerTest`](../src/test/java/com/payflow/notification/consumer/PaymentEventConsumerTest.java) exercises these `eventType` values:

| `eventType` | Meaning (from payments BC) |
| --- | --- |
| `payment.created` | Payment persisted, pending capture |
| `payment.captured` | Capture completed |
| `payment.cancelled` | Cancelled before capture |
| `payment.refunded` | Refund recorded |
| `payment.expired` | Pending payment expired |

## Behaviour

| Case | Behaviour |
| --- | --- |
| Valid JSON → `PaymentEventEnvelope` | Log at INFO; call **webhook-service** dispatch (`WebhookDispatchClient`) when `payflow.webhook-dispatch.enabled` is true |
| Malformed JSON | Log at WARN; no exception propagation from the listener; no DLQ in this service |

Configure outbound dispatch with `payflow.webhook-dispatch.base-url` and `payflow.webhook-dispatch.enabled` (see [`application.yml`](../src/main/resources/application.yml)).

## Related documentation

- Payment aggregate and events: [`payment-service/docs/domain-model.md`](../../payment-service/docs/domain-model.md)
- Webhook registration and delivery: [`webhook-service/docs/domain-model.md`](../../webhook-service/docs/domain-model.md)
