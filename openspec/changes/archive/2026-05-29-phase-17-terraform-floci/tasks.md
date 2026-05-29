# Tasks: Phase 17 — Infrastructure as Code with Terraform + Floci

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Terraform + compose (foundation) → PR 2: Spring Cloud AWS (app config) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Terraform modules + docker-compose + .gitignore | PR 1 | ~340 lines; infra foundation, independent |
| 2 | Maven BOM + service POMs + application.yml | PR 2 | ~80 lines; depends on PR 1 for secret path contracts |
| 3 | CI workflow + manual verification | PR 3 | ~35 lines; depends on PR 1 and PR 2 |

## Phase 1: Foundation — Terraform Modules

- [x] 1.1 Create `infra/terraform/main.tf` — AWS provider → localhost:4566, 4 module references
- [x] 1.2 Create `infra/terraform/variables.tf` — `aws_region`, `environment`, `db_password` (sensitive)
- [x] 1.3 Create `infra/terraform/outputs.tf` — `kms_key_arn`, 4 secret ARNs, `bootstrap_brokers`, `registry_url`
- [x] 1.4 Create `infra/terraform/modules/security/` — KMS key + alias, 4 Secrets Manager secrets, IAM policies
- [x] 1.5 Create `infra/terraform/modules/data/` — 3 RDS PostgreSQL (Floci-managed PG containers)
- [x] 1.6 Create `infra/terraform/modules/messaging/` — MSK cluster (Redpanda-backed)
- [x] 1.7 Create `infra/terraform/modules/registry/` — 5 ECR repos (payment, merchant, webhook, notification, frontend)
- [x] 1.8 Create `infra/docker-compose-floci.yml` — all existing services + Floci (4566, 7001-7003, docker socket, healthcheck)
- [x] 1.9 Create `infra/terraform/terraform.tfvars.local` — local `db_password` override (gitignored)
- [x] 1.10 Modify `.gitignore` — add `*.tfstate`, `.terraform/`, `crash.log`, `terraform.tfvars.local`

## Phase 2: Core Implementation — Spring Cloud AWS

- [x] 2.1 Modify `backend/pom.xml` — add `spring-cloud-aws-dependencies` BOM `2023.0.x` in `<dependencyManagement>` (version: 3.2.1 — corrects `2023.0.3` to Spring Cloud AWS `3.2.1` for Spring Boot 3.3.6 compat)
- [x] 2.2 Modify 4 service POMs — add `spring-cloud-aws-starter-secrets-manager` (no version, inherits BOM)
- [x] 2.3 Modify 3 DB-service `application.yml` — replace `spring.datasource.*` with `optional:aws-secretsmanager:/payflow/${ENVIRONMENT:local}/{service}/db?prefix=db.`
- [x] 2.4 Modify `notification-service/application.yml` — replace `spring.kafka.bootstrap-servers` with `optional:aws-secretsmanager:/payflow/${ENVIRONMENT:local}/kafka`

## Phase 3: CI + Verification

- [x] 3.1 Create `.github/workflows/infra-ci.yml` — trigger on `infra/terraform/**`, run `fmt -check`, `init -backend=false`, `validate`
- [ ] 3.2 **[MANUAL]** Verify: `terraform apply` with Floci → KMS, 4 secrets, 3 RDS, 1 MSK, 5 ECR all provision
- [ ] 3.3 **[MANUAL]** Verify: all 4 services start and fetch creds from Secrets Manager — zero hardcoded values in logs
