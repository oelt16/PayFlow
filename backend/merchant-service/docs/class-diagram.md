# merchant-service class diagram

The **merchant-service** Maven module exists for multi-module layout; **implementation is deferred** to Phase 5 per the PayFlow specification.

No Spring Boot application, REST controllers, or infrastructure adapters are present yet. This diagram reserves the expected hexagonal layout.

```mermaid
classDiagram
  direction TB

  namespace future_domain {
    class Merchant <<aggregate root>> {
      <<Phase 5>>
    }
    class MerchantId <<value object>> {
      <<Phase 5>>
    }
  }

  namespace future_application {
    class MerchantApplicationService {
      <<Phase 5>>
    }
    class MerchantRepository {
      <<interface>>
    }
  }

  note for MerchantRepository "Phase 5 persistence port"

  namespace future_api {
    class MerchantsController {
      <<Phase 5>>
    }
  }

  namespace future_infrastructure {
    class JpaMerchantRepositoryAdapter {
      <<Phase 5>>
    }
  }

  Merchant ..> MerchantId
  MerchantApplicationService ..> MerchantRepository
  MerchantsController ..> MerchantApplicationService
  JpaMerchantRepositoryAdapter ..|> MerchantRepository
```

## Notes

- Replace this file when Phase 5 adds real types under `src/main/java/com/payflow/merchant/`.
- Until then, see [`domain-model.md`](domain-model.md) for the intended domain sketch.
