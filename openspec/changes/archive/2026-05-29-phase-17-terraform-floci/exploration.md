# Exploration: Phase 17 — Infrastructure as Code with Terraform + Floci

## Current State

### Infrastructure (`infra/`)
- No `infra/terraform/` directory exists — **clean slate**
- `docker-compose.yml` defines 10 services: postgres, kafka, kafka-ui, payment-service, merchant-service, webhook-service, notification-service, webhook-receiver, frontend, prometheus, grafana, zipkin
- **No Floci service** in docker-compose
- `infra/k8s/` has raw K8s manifests for all components (namespace, secrets, configmaps, deployments, services, ingress, HPA, PVC)
- No Helm charts exist anywhere — v2.0 used raw `kubectl apply`
- No `.tf` files, no `.hcl` files, no `.tfvars` files exist anywhere in the repo

### Service Ports
| Service | Port | Exposed |
|---------|------|---------|
| payment-service | 8081 | Yes |
| merchant-service | 8082 | Yes |
| webhook-service | 8083 | Yes |
| notification-service | 8084 | No (internal only) |
| frontend | 3000 | Yes |
| postgres | 5432 | Yes |
| kafka | 9092 | Yes |

### Hardcoded Credentials in application.yml
All 3 DB-using services have **hardcoded** credentials in `src/main/resources/application.yml`:

1. **payment-service** (lines 20-22):
   - `spring.datasource.url: jdbc:postgresql://localhost:5432/payflow`
   - `spring.datasource.username: payflow`
   - `spring.datasource.password: payflow`

2. **merchant-service** (lines 15-17):
   - `spring.datasource.url: jdbc:postgresql://localhost:5432/payflow`
   - `spring.datasource.username: payflow`
   - `spring.datasource.password: payflow`

3. **webhook-service** (lines 15-17):
   - `spring.datasource.url: jdbc:postgresql://localhost:5432/payflow?currentSchema=webhooks`
   - `spring.datasource.username: payflow`
   - `spring.datasource.password: payflow`

4. **notification-service**: No datasource (Kafka-only consumer)

All 4 services have hardcoded `spring.kafka.bootstrap-servers: localhost:9092`

### Maven Dependencies
- **Root POM** (`backend/pom.xml`): 4 modules, Spring Boot 3.3.6, Java 21, no cloud BOM
- **No Spring Cloud AWS dependencies** exist in any service POM
- Each service has: spring-boot-starter-web/data-jpa/validation, spring-kafka, flyway, postgresql, springdoc, actuator, micrometer, caffeine, bucket4j

### CI Workflows (`.github/workflows/`)
- `backend-ci.yml` — Maven verify + Docker build/push to ghcr.io
- `frontend-ci.yml` — Lint + Test + Build + Docker build/push
- **No infra-ci.yml** exists — needs creation

### .gitignore
- No Terraform entries (`.tfstate`, `.terraform/`, `*.tfvars.local`, `*.tfvars` are NOT ignored)

### K8s Secrets
- `infra/k8s/secrets.yml` has Opaque secret `payflow-db` with `POSTGRES_USER: payflow`, `POSTGRES_PASSWORD: change-me-in-production`
- All service deployments reference this secret via `secretKeyRef`

### OpenSpec / SDD
- `openspec/config.yaml` exists with full project context
- `openspec/changes/` has 7 completed/active change folders
- No `phase-17-terraform-floci` folder existed — now created
- Engine mode: `openspec`

## Affected Areas

### New Files to Create
```
infra/terraform/
├── main.tf                    # Root module: providers, backend
├── variables.tf               # Input variables (region, env, passwords)
├── outputs.tf                 # Outputs (kms key arn, secret arns, bootstrap brokers)
├── terraform.tfvars.local     # Local overrides (gitignored)
└── modules/
    ├── security/
    │   └── main.tf            # KMS key + alias, Secrets Manager secrets (3 DB secrets)
    ├── data/
    │   └── main.tf            # RDS instances: payment, merchant, webhook
    ├── messaging/
    │   └── main.tf            # MSK cluster (Redpanda)
    └── registry/
        └── main.tf            # ECR repositories for 5 images (4 services + frontend)

.github/workflows/infra-ci.yml  # Terraform fmt + validate on PR
```

### Files to Modify
| File | Change |
|------|--------|
| `infra/docker-compose.yml` | Add `floci` service, optionally remove standalone postgres/kafka |
| `backend/payment-service/pom.xml` | Add `spring-cloud-aws-starter-secrets-manager` |
| `backend/merchant-service/pom.xml` | Add `spring-cloud-aws-starter-secrets-manager` |
| `backend/webhook-service/pom.xml` | Add `spring-cloud-aws-starter-secrets-manager` |
| `backend/pom.xml` (root) | Add `spring-cloud-aws-dependencies` BOM to dependencyManagement |
| `backend/payment-service/src/main/resources/application.yml` | Replace hardcoded DB/Kafka with Secrets Manager `${...}` refs, add `spring.cloud.aws.*` config, add `spring.config.import` |
| `backend/merchant-service/src/main/resources/application.yml` | Same as above |
| `backend/webhook-service/src/main/resources/application.yml` | Same as above |
| `backend/notification-service/src/main/resources/application.yml` | Add `spring.cloud.aws.*` config (no DB secret needed) |
| `.gitignore` | Add Terraform patterns: `*.tfstate`, `*.tfstate.*`, `.terraform/`, `*.tfvars.local`, `crash.log`, `override.tf`, `override.tf.json` |

## Key Findings

### 1. All 3 DB-using services share the SAME credentials
Currently all connect as `payflow/payflow` to the same `payflow` database. The spec creates **separate DB instances per service** with different schemas and users. This is a security improvement but means:
- The current app uses Flyway schemas (`payments`, `merchants`, `webhooks`) within a single DB
- After Phase 17, each service will have its own PostgreSQL instance with its own schema
- **The application code itself needs NO changes** — just the datasource URL and credentials

### 2. Flyway still handles schema creation
The spec defines `db_name` per RDS instance, but Flyway is already configured with `create-schemas: true` and named schemas per service. The Flyway migrations will create the schemas on first startup. **No Flyway config changes needed.**

### 3. Notification-service has NO database
It only connects to Kafka. Its `application.yml` changes are limited to adding `spring.cloud.aws.*` endpoint config (for potential future Secrets Manager use, though it doesn't need secrets currently).

### 4. The spec uses a shared `db_password` variable for ALL instances
The spec defines a single `var.db_password` used for all 3 RDS instances AND all 3 Secrets Manager secrets. In production you'd want per-service passwords, but for local dev with Floci, a shared password is acceptable.

### 5. Kafka bootstrap address changes
- Currently: `localhost:9092` (hardcoded)
- After Phase 17: The spec doesn't explicitly show the Kafka config change. Floci MSK would expose a bootstrap address. The `aws_msk_cluster` output gives `bootstrap_brokers`. Either the MSK bootstrap address flows through Secrets Manager or remains as an env var.

**Critical design question**: Should Kafka config also go through Secrets Manager, or stay as env vars? The spec is silent on this. Phase 17 as written only migrates DB credentials to Secrets Manager.

### 6. Floci RDS standalone mode vs. docker-compose postgres
Floci RDS manages **real PostgreSQL 16 containers** proxied on ports 7001-7003. This means:
- The standalone `postgres` service in docker-compose becomes redundant for Terraform-provisioned environments
- But the dev startup sequence changes: Floci must be up FIRST before `terraform apply`, then services start
- Consider: keep standalone postgres for quick dev, use Floci RDS for infra-provisioned dev

### 7. No test profile changes needed
Test profiles (`application-test.yml`, `application-ratelimit-test.yml`) use Testcontainers, not real databases. They don't reference hardcoded passwords. **No test config changes needed.**

### 8. .gitignore needs Terraform entries
Currently no Terraform artifacts are ignored. Must add at minimum:
- `*.tfstate` / `*.tfstate.*`
- `.terraform/`
- `*.tfvars.local`
- `crash.log`

### 9. Floci image tag uncertainty
The spec uses `floci/floci:latest`. Need to verify: is this the correct image name? Floci is newer than LocalStack. The image may need adjustment if the real name differs.

### 10. Phase 17 does NOT touch K8s manifests
The spec explicitly defers K8s changes to Phase 18 (Helm). Phase 17 is Terraform + docker-compose only. The existing `infra/k8s/` manifests remain untouched.

## Ready for Proposal
**Yes** — the scope is well-defined by the spec, and the investigation reveals no blockers. The orchestrator should proceed with `sdd-propose`.

Key message for the orchestrator:
- Clean slate for Terraform — no existing `.tf` files to conflict
- 3 POM files need the Spring Cloud AWS dependency added
- 3 main application.yml files need secret migration
- 1 new CI workflow needed
- The notication-service needs minimal changes (only `spring.cloud.aws.*` endpoint config, no secret import since it has no DB)
- Consider whether Kafka bootstrap should also migrate to Secrets Manager or stay as env var
- .gitignore updates must happen alongside Terraform creation
