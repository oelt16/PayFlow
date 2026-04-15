# webhook-service class diagram

Hexagonal view: domain, application (ports and service), API, and infrastructure. Mermaid source; render in GitHub, GitLab, or an IDE Mermaid preview.

```mermaid
classDiagram
  direction TB

  namespace domain {
    class WebhookEndpoint
    class WebhookDelivery
    class WebhookId
    class WebhookDeliveryId
    class MerchantId
    class DeliveryStatus <<enumeration>>
    abstract class DomainException
    class InvalidWebhookUrlException
    class MaxWebhookEndpointsExceededException
  }

  namespace application {
    class WebhookApplicationService
    <<interface>> WebhookEndpointRepository
    <<interface>> WebhookDeliveryRepository
    <<interface>> WebhookSender
    <<interface>> WebhookDlqPublisher
    class WebhookSendResult
    class WebhookProperties
    class InternalDispatchProperties
    class KafkaTopicProperties
    class RegisteredWebhook
    class WebhookNotFoundException
  }

  namespace api {
    class WebhooksController
    class InternalWebhookDispatchController
    class WebhookApiMapper <<utility>>
    class ApiExceptionHandler
    class RegisterWebhookRequest
    class WebhookRegisteredResponse
    class WebhookSummaryResponse
    class WebhookListResponse
    class DeliveryResponse
    class DeliveryListResponse
    class DispatchRequest
    class ApiKeyAuthenticationFilter
    class RequestIdFilter
    class MerchantContext
    class JdbcApiKeyAuthenticator
  }

  namespace infrastructure.persistence {
    class JpaWebhookEndpointRepositoryAdapter
    class JpaWebhookDeliveryRepositoryAdapter
    class WebhookPersistenceMapper
    class WebhookEndpointJpaEntity
    <<interface>> WebhookEndpointSpringDataRepository
    class WebhookDeliveryJpaEntity
    <<interface>> WebhookDeliverySpringDataRepository
  }

  namespace infrastructure.http {
    class HmacSha256WebhookSender
  }

  namespace infrastructure.kafka {
    class KafkaWebhookDlqPublisher
    class NoOpWebhookDlqPublisher
  }

  namespace infrastructure.scheduling {
    class WebhookDeliveryRetryJob
  }

  WebhookEndpoint *-- WebhookId
  WebhookEndpoint *-- MerchantId
  WebhookDelivery *-- WebhookDeliveryId
  WebhookDelivery *-- WebhookId
  WebhookDelivery *-- DeliveryStatus

  DomainException <|-- InvalidWebhookUrlException
  DomainException <|-- MaxWebhookEndpointsExceededException

  WebhookApplicationService ..> WebhookEndpointRepository
  WebhookApplicationService ..> WebhookDeliveryRepository
  WebhookApplicationService ..> WebhookSender
  WebhookApplicationService ..> WebhookDlqPublisher
  WebhookApplicationService ..> WebhookProperties
  WebhookApplicationService ..> WebhookEndpoint
  WebhookApplicationService ..> WebhookDelivery
  WebhookApplicationService ..> RegisteredWebhook
  WebhookApplicationService ..> WebhookNotFoundException

  WebhookSender ..> WebhookSendResult
  HmacSha256WebhookSender ..|> WebhookSender

  KafkaWebhookDlqPublisher ..|> WebhookDlqPublisher
  NoOpWebhookDlqPublisher ..|> WebhookDlqPublisher
  KafkaWebhookDlqPublisher ..> KafkaTopicProperties

  JpaWebhookEndpointRepositoryAdapter ..|> WebhookEndpointRepository
  JpaWebhookDeliveryRepositoryAdapter ..|> WebhookDeliveryRepository
  JpaWebhookEndpointRepositoryAdapter ..> WebhookEndpointSpringDataRepository
  JpaWebhookDeliveryRepositoryAdapter ..> WebhookDeliverySpringDataRepository
  JpaWebhookEndpointRepositoryAdapter ..> WebhookPersistenceMapper
  JpaWebhookDeliveryRepositoryAdapter ..> WebhookPersistenceMapper
  WebhookPersistenceMapper ..> WebhookEndpoint
  WebhookPersistenceMapper ..> WebhookDelivery
  WebhookPersistenceMapper ..> WebhookEndpointJpaEntity
  WebhookPersistenceMapper ..> WebhookDeliveryJpaEntity

  WebhooksController ..> WebhookApplicationService
  WebhooksController ..> WebhookApiMapper
  InternalWebhookDispatchController ..> WebhookApplicationService
  InternalWebhookDispatchController ..> InternalDispatchProperties
  WebhookApiMapper ..> WebhookEndpoint
  WebhookApiMapper ..> WebhookDelivery
  WebhookApiMapper ..> RegisteredWebhook

  ApiExceptionHandler ..> WebhookNotFoundException
  ApiExceptionHandler ..> DomainException

  WebhookDeliveryRetryJob ..> WebhookApplicationService
```

## Notes

- **Implementation** (`..|>`): infrastructure types implement application ports. `NoOpWebhookDlqPublisher` is used when no `KafkaTemplate` bean exists (for example tests with Kafka auto-config excluded).
- **Internal API:** `InternalWebhookDispatchController` is not protected by `ApiKeyAuthenticationFilter` (only `/v1/*` is); protect it at the network layer in production.
- Boot helpers (`WebhookServiceApplication`, `TimeConfig`, `WebhookHttpConfig`, `RestClient.Builder`) are omitted for readability.
