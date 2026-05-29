# Design: Phase 17 — Infrastructure as Code with Terraform + Floci

## Technical Approach

Four Terraform modules provision local AWS resources via Floci at `http://floci:4566`. Spring Cloud AWS replaces hardcoded DB/Kafka credentials with Secrets Manager lookups at startup. A new `docker-compose-floci.yml` copies all services from the original compose + adds Floci, with a 3-step startup: Floci → `terraform apply` → services. Original `docker-compose.yml` untouched — rollback is a `git revert`.

## Architecture Decisions

| Option | Tradeoff | Choice |
|--------|----------|--------|
| **Terraform backend**: local vs S3 | S3 needs real AWS; local works offline but state isn't shared | **Local** — no real AWS account yet |
| **Secret resolution**: Spring Cloud AWS vs manual AWS SDK calls | Manual gives control but adds code; Spring Cloud AWS is declarative (`spring.config.import`) | **Spring Cloud AWS** — zero Java code, property-source pattern |
| **Secret layout**: per-service vs one shared secret | Shared is simpler; per-service follows least-privilege | **Per-service** — each service reads only its own path |
| **Standalone PG/Kafka**: keep in floci compose vs remove | Removing forces Floci for all dev; keeping provides quick path | **Keep both** — `depends_on` follows Floci health for the Terraform path |
| **Floci image tag**: `latest` vs pinned | Pinned is reproducible; `latest` is simpler and vetted (50K+ pulls) | **`latest`** — documented in compose; can pin later |

## Data Flow

```
Floci container ──port 4566──→ Terraform apply ──→ Resources (KMS, Secrets, RDS, MSK, ECR)
                                                      │
     Service startup ──spring.config.import──→ Secrets Manager ──→ DB/Kafka creds
                                                      │
                                              Floci RDS proxy ──port 7001-7003──→ PostgreSQL containers
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `infra/terraform/main.tf` | Create | Root module — provider config, module calls |
| `infra/terraform/variables.tf` | Create | `aws_region`, `environment`, `db_password` |
| `infra/terraform/outputs.tf` | Create | `kms_key_arn`, 4 secret ARNs, `bootstrap_brokers`, `registry_url` |
| `infra/terraform/security/main.tf` | Create | KMS key + alias, 4 secrets (`payment-db`, `merchant-db`, `webhook-db`, `kafka`) |
| `infra/terraform/security/variables.tf` | Create | Module inputs |
| `infra/terraform/security/outputs.tf` | Create | Secret ARNs, KMS key ARN |
| `infra/terraform/data/main.tf` | Create | 3 RDS PostgreSQL instances via Floci RDS proxy |
| `infra/terraform/data/variables.tf` | Create | Module inputs |
| `infra/terraform/data/outputs.tf` | Create | RDS endpoints |
| `infra/terraform/messaging/main.tf` | Create | MSK cluster (Redpanda-backed) |
| `infra/terraform/messaging/outputs.tf` | Create | `bootstrap_brokers` |
| `infra/terraform/registry/main.tf` | Create | 5 ECR repos (payment, merchant, webhook, notification, frontend) |
| `infra/terraform/registry/outputs.tf` | Create | `registry_url` |
| `infra/docker-compose-floci.yml` | Create | All 12 services from `docker-compose.yml` + Floci (4566, 7001-7003, docker socket, healthcheck) |
| `.github/workflows/infra-ci.yml` | Create | `terraform fmt -check` + `init -backend=false` + `validate` on PR |
| `backend/pom.xml` | Modify | Add `spring-cloud-aws-dependencies` BOM `2023.0.3` in `<dependencyManagement>` |
| `backend/*/pom.xml` (x4) | Modify | Add `spring-cloud-aws-starter-secrets-manager` (no version — inherits BOM) |
| `backend/*/src/main/resources/application.yml` (x4) | Modify | Replace `spring.datasource.*` and `spring.kafka.bootstrap-servers` with `aws-secretsmanager:` refs |
| `.gitignore` | Modify | Add `*.tfstate`, `*.tfstate.*`, `.terraform/`, `crash.log`, `override.tf`, `terraform.tfvars.local` |

## Interfaces / Contracts

**Secret paths** (created by Terraform, read by Spring Cloud AWS):

| Secret Path | Read By | Content |
|---|---|---|
| `/payflow/${ENVIRONMENT}/payment-service/db` | payment-service | JDBC URL + username + password |
| `/payflow/${ENVIRONMENT}/merchant-service/db` | merchant-service | JDBC URL + username + password |
| `/payflow/${ENVIRONMENT}/webhook-service/db` | webhook-service | JDBC URL + username + password |
| `/payflow/${ENVIRONMENT}/kafka` | notification-service | `bootstrap-servers` |

**Spring Cloud AWS config** (identical across all 4 services):

```yaml
spring:
  config:
    import: aws-secretsmanager:/payflow/${ENVIRONMENT:local}/${spring.application.name}/db
  cloud:
    aws:
      endpoint: http://floci:4566
      region:
        static: us-east-1
      credentials:
        access-key: test
        secret-key: test
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Terraform fmt | All `.tf` files formatted | `terraform fmt -check -recursive` in CI |
| Terraform validate | Module syntax + internal refs | `terraform init -backend=false && terraform validate` in CI |
| Manual verify | Services start + fetch creds | `docker compose -f docker-compose-floci.yml up` → check logs for Secrets Manager resolution |

No new application tests — test profiles use Testcontainers, not real DBs.

## Migration / Rollout

- **Rollback**: `git revert` the change; `docker compose -f infra/docker-compose.yml up` works unchanged
- **Data**: Flyway unchanged — no schema migration
- **Startup flow**:
  1. `docker compose -f infra/docker-compose-floci.yml up -d floci`
  2. `terraform -chdir=infra/terraform apply`
  3. `docker compose -f infra/docker-compose-floci.yml up --build`

## Open Questions

None.
