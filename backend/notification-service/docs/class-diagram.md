# notification-service class diagram

Kafka consumer on `payments.events`, JSON mapping to `PaymentEventEnvelope`, logging, and **HTTP dispatch** to webhook-service. Mermaid source; render in GitHub, GitLab, or an IDE Mermaid preview.

```mermaid
classDiagram
  direction TB

  namespace bootstrap {
    class NotificationServiceApplication
  }

  namespace config {
    class NotificationHttpConfig
  }

  namespace properties {
    class WebhookDispatchProperties
  }

  namespace event {
    class PaymentEventEnvelope <<record>> {
      +eventId String
      +eventType String
      +aggregateId String
      +merchantId String
      +occurredAt Instant
      +payload Map~String,Object~
    }
  }

  namespace webhook {
    class WebhookDispatchClient {
      +notifyWebhookService(PaymentEventEnvelope envelope)
    }
  }

  namespace consumer {
    class PaymentEventConsumer {
      +listen(String value)
      ~processEnvelopeJson(String value)
    }
  }

  NotificationServiceApplication ..> PaymentEventConsumer : component scan
  NotificationServiceApplication ..> WebhookDispatchProperties : EnableConfigurationProperties

  class RestClientBuilder <<Spring>>
  note for RestClientBuilder "Spring RestClient.Builder bean from NotificationHttpConfig"

  NotificationHttpConfig ..> RestClientBuilder : @Bean

  WebhookDispatchClient ..> RestClientBuilder : builds RestClient
  WebhookDispatchClient ..> WebhookDispatchProperties
  WebhookDispatchClient ..> ObjectMapper
  WebhookDispatchClient ..> PaymentEventEnvelope : builds dispatch JSON body

  PaymentEventConsumer ..> ObjectMapper
  PaymentEventConsumer ..> PaymentEventEnvelope : readValue
  PaymentEventConsumer ..> WebhookDispatchClient : after successful parse
```

## Notes

- **Dependency** (`..>`): `PaymentEventConsumer` uses Jackson `ObjectMapper` and, on success, `WebhookDispatchClient`.
- **Entry point:** Spring Kafka invokes `PaymentEventConsumer.listen` via `@KafkaListener(topics = "payments.events", groupId = "notification-service")`.
- **`RestClient`:** Provided by Spring; `NotificationHttpConfig` exposes `RestClient.Builder` as a bean. The diagram uses `RestClientBuilder` as a label for that Spring type.
- **Tests** under `src/test/java/com/payflow/notification/` are omitted to keep the diagram small (`PaymentEventConsumerTest`, Kafka and WireMock integration tests).
