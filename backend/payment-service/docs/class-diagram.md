# payment-service class diagram

Hexagonal view: domain, application (ports and use cases), API, and infrastructure. Mermaid source; render in GitHub, GitLab, or an IDE Mermaid preview.

```mermaid
classDiagram
  direction TB

  namespace domain {
    class Payment {
      +create(...)
      +restore(...)
      +capture(now)
      +cancel(now, reason)
      +refund(refundAmount, now)
      +expire(now)
      +pullDomainEvents() List~DomainEvent~
    }
    class Money
    class CardDetails
    class PaymentId
    class MerchantId
    class RefundId
    class PaymentStatus <<enumeration>>
    class CardBrand <<enumeration>>
    <<interface>> DomainEvent
    class PaymentCreatedEvent
    class PaymentCapturedEvent
    class PaymentCancelledEvent
    class PaymentRefundedEvent
    class PaymentExpiredEvent
    abstract class DomainException
    class InvalidStateTransitionException
    class InsufficientRefundableAmountException
    class InvalidCurrencyException
    class NegativeAmountException
  }

  namespace application {
    class PaymentApplicationService
    <<interface>> PaymentRepository
    <<interface>> DomainEventOutbox
    <<interface>> AcquiringPort
    class ClientSecretGenerator
    class CreatePaymentCommand
    class CreatedPaymentResult
    class PageRequest
    class PageResult~T~
    class PaymentNotFoundException
    class StubCardDetailsFactory
    class MoneyMinorUnits
  }

  namespace api {
    class PaymentsController
    class PaymentApiMapper <<utility>>
    class CreatePaymentRequest
    class CardPayload
    class PaymentResponse
    class CardResponse
    class PaymentListResponse
    class CancelPaymentRequest
    class ApiExceptionHandler
    class ApiKeyAuthenticationFilter
    class RequestIdFilter
    class MerchantContext
    class PayflowSecurityProperties
  }

  namespace infrastructure.persistence {
    class JpaPaymentRepositoryAdapter
    class PaymentPersistenceMapper
    class PaymentJpaEntity
    <<interface>> PaymentSpringDataRepository
    class OutboxEventJpaEntity
    <<interface>> OutboxEventSpringDataRepository
  }

  namespace infrastructure.outbox {
    class TransactionalOutboxAppender
    class OutboxEventPayloadMapper
  }

  namespace infrastructure.acquiring {
    class NoOpAcquiringAdapter
  }

  Payment *-- PaymentId
  Payment *-- MerchantId
  Payment *-- Money
  Payment *-- CardDetails
  Payment *-- PaymentStatus
  Payment o-- DomainEvent : records
  CardDetails *-- CardBrand

  DomainEvent <|.. PaymentCreatedEvent
  DomainEvent <|.. PaymentCapturedEvent
  DomainEvent <|.. PaymentCancelledEvent
  DomainEvent <|.. PaymentRefundedEvent
  DomainEvent <|.. PaymentExpiredEvent
  PaymentRefundedEvent ..> RefundId

  DomainException <|-- InvalidStateTransitionException
  DomainException <|-- InsufficientRefundableAmountException
  DomainException <|-- InvalidCurrencyException
  DomainException <|-- NegativeAmountException

  PaymentApplicationService ..> PaymentRepository
  PaymentApplicationService ..> DomainEventOutbox
  PaymentApplicationService ..> AcquiringPort
  PaymentApplicationService ..> ClientSecretGenerator
  PaymentApplicationService ..> CreatePaymentCommand
  PaymentApplicationService ..> CreatedPaymentResult
  PaymentApplicationService ..> StubCardDetailsFactory
  PaymentApplicationService ..> MoneyMinorUnits
  PaymentApplicationService ..> PageRequest
  PaymentApplicationService ..> PageResult
  PaymentApplicationService ..> Payment
  PaymentApplicationService ..> PaymentNotFoundException

  PaymentsController ..> PaymentApplicationService
  PaymentsController ..> CreatePaymentCommand
  PaymentsController ..> PaymentApiMapper
  PaymentsController ..> CreatePaymentRequest
  PaymentsController ..> CancelPaymentRequest
  CreatePaymentRequest *-- CardPayload
  PaymentResponse *-- CardResponse
  PaymentListResponse o-- PaymentResponse
  PaymentApiMapper ..> Payment
  PaymentApiMapper ..> MoneyMinorUnits

  ApiExceptionHandler ..> PaymentNotFoundException
  ApiExceptionHandler ..> DomainException

  JpaPaymentRepositoryAdapter ..|> PaymentRepository
  JpaPaymentRepositoryAdapter ..> PaymentSpringDataRepository
  JpaPaymentRepositoryAdapter ..> PaymentPersistenceMapper
  PaymentPersistenceMapper ..> Payment
  PaymentPersistenceMapper ..> PaymentJpaEntity
  PaymentSpringDataRepository ..> PaymentJpaEntity
  PaymentJpaEntity ..> PaymentStatus
  PaymentJpaEntity ..> CardBrand

  TransactionalOutboxAppender ..|> DomainEventOutbox
  TransactionalOutboxAppender ..> OutboxEventSpringDataRepository
  TransactionalOutboxAppender ..> OutboxEventPayloadMapper
  TransactionalOutboxAppender ..> DomainEvent
  OutboxEventPayloadMapper ..> DomainEvent
  OutboxEventSpringDataRepository ..> OutboxEventJpaEntity

  NoOpAcquiringAdapter ..|> AcquiringPort
  NoOpAcquiringAdapter ..> PaymentId
  NoOpAcquiringAdapter ..> Money
  NoOpAcquiringAdapter ..> MerchantId
```

## Notes

- **Implementation** (`..|>`): infrastructure types implement application ports.
- **Dependency** (`..>`): uses another type without owning it.
- **Composition** (`*--`): `Payment` holds value objects and records `DomainEvent` instances until the application layer pulls them for the outbox.
- Boot and cross-cutting types (`PaymentServiceApplication`, `TimeConfig`) are omitted to keep the diagram readable.
