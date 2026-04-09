# merchant-service class diagram

Hexagonal layout under `com.payflow.merchant`:

```mermaid
classDiagram
  direction TB

  namespace domain {
    class Merchant <<aggregate root>>
    class MerchantId <<value object>>
    class ApiKeyHash <<value object>>
    class MerchantCreatedEvent
    class MerchantDeactivatedEvent
  }

  namespace application {
    class MerchantApplicationService
    interface MerchantRepository
    interface DomainEventOutbox
    interface ApiKeyHasher
  }

  namespace api {
    class MerchantsController
    class ApiKeyAuthenticationFilter
    class MerchantContext
    class ApiExceptionHandler
  }

  namespace infrastructure {
    class JpaMerchantRepositoryAdapter
    class TransactionalOutboxAppender
    class OutboxRelay
    class BCryptApiKeyHasher
  }

  Merchant --> MerchantId
  Merchant --> ApiKeyHash
  MerchantApplicationService ..> MerchantRepository
  MerchantApplicationService ..> DomainEventOutbox
  MerchantApplicationService ..> ApiKeyHasher
  MerchantsController --> MerchantApplicationService
  ApiKeyAuthenticationFilter --> MerchantApplicationService
  JpaMerchantRepositoryAdapter ..|> MerchantRepository
  TransactionalOutboxAppender ..|> DomainEventOutbox
  BCryptApiKeyHasher ..|> ApiKeyHasher
```

## REST (public)

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/v1/merchants` | None (registration) |
| `GET` | `/v1/merchants/me` | Bearer API key |
| `DELETE` | `/v1/merchants/me` | Bearer API key |
| `POST` | `/v1/merchants/me/api-keys` | Bearer API key |
