# Phase 17 — Infrastructure as Code with Terraform + Floci

> **Part of**: PayFlow v3.0 Cloud-Native & Reliability Specification
> **Builds on**: Phase 16 (OpenAPI 3.1 Documentation & Swagger UI)
> **Next**: Phase 18 (Production-Grade Kubernetes with kind + Helm)

---

## What & Why

Phase 17 replaces all hardcoded database and Kafka credentials with AWS Secrets Manager (emulated by Floci), and provisions all infrastructure through Terraform instead of manual Docker Compose. The same `.tf` files work locally (pointed at `localhost:4566`) and in real AWS (removing the `endpoint_url` override).

**Why this matters for interviews**: "I write Terraform" and opening a `.tf` file is incomparably stronger than "I edit YAML files manually." Every infrastructure decision uses real AWS SDK calls against Floci — no stub, no mock, no shortcut.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Developer Machine                      │
│                                                          │
│  docker-compose-floci.yml  ───  docker compose up -d     │
│       │                                                  │
│       v                                                  │
│  ┌──────────┐    ┌──────────────────────────────────────┐│
│  │  Floci   │    │  Application Services                ││
│  │  :4566   │    │  payment :8081    merchant :8082     ││
│  │  AWS API │    │  webhook :8083    notification       ││
│  └────┬─────┘    │  frontend :3000                      ││
│       │          └──────────────────────────────────────┘│
│       │                      │                           │
│  ┌────v──────────────────────v──────┐                    │
│  │  Terraform provisions:           │                    │
│  │  ├─ KMS key (encryption)        │                    │
│  │  ├─ 4 Secrets Manager secrets   │                    │
│  │  ├─ 3 RDS PostgreSQL instances  │                    │
│  │  ├─ 1 MSK/Redpanda cluster     │                    │
│  │  └─ 5 ECR repositories         │                    │
│  └─────────────────────────────────┘                    │
└──────────────────────────────────────────────────────────┘
```

---

## What Changed

### New Files (15)

| File | Purpose |
|------|---------|
| `infra/terraform/main.tf` | Root module — AWS provider → localhost:4566, 4 module references |
| `infra/terraform/variables.tf` | `aws_region`, `environment`, `db_password` (sensitive) |
| `infra/terraform/outputs.tf` | 7 outputs: KMS ARN, 4 secret ARNs, brokers, registry URL |
| `infra/terraform/modules/security/main.tf` | KMS key + alias, 4 secrets, 4 IAM policies |
| `infra/terraform/modules/data/main.tf` | 3 RDS PostgreSQL (ports 7001-7003) |
| `infra/terraform/modules/messaging/main.tf` | MSK/Redpanda cluster |
| `infra/terraform/modules/registry/main.tf` | 5 ECR repos |
| `infra/docker-compose-floci.yml` | All services + Floci |
| `.github/workflows/infra-ci.yml` | Terraform format + validate on PR |
| `infra/terraform/terraform.tfvars.local` | Local db_password override (gitignored) |
| `scripts/set-aws-env.ps1` | PowerShell — load AWS env vars for Floci |
| `scripts/set-aws-env.sh` | Bash — load AWS env vars for Floci (macOS/Linux) |

### Modified Files (10)

| File | Change |
|------|--------|
| `backend/pom.xml` | Added Spring Cloud AWS BOM v3.2.1 |
| 4 service `pom.xml` | Added `spring-cloud-aws-starter-secrets-manager` |
| 4 service `application.yml` | DB/Kafka creds → Secrets Manager refs |
| `.gitignore` | Added 6 Terraform patterns |

### Key Config Change (application.yml)

```yaml
# BEFORE (Phase 16)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payflow
    username: payflow
    password: payflow

# AFTER (Phase 17)
spring:
  config:
    import: "optional:aws-secretsmanager:/payflow/${ENVIRONMENT:local}/payment-service/db?prefix=db."
  datasource:
    url: ${db.url}          # ← resuelto desde Secrets Manager
    username: ${db.username}
    password: ${db.password}
```

The `optional:` prefix allows services to start without Floci.
The `?prefix=db.` suffix maps secret JSON keys (`url`, `username`, `password`) to `${db.url}`, `${db.username}`, `${db.password}`.

---

## How to Test

### Prerequisites

| Tool | Install |
|------|---------|
| Docker Compose | ✅ Ya instalado |
| Terraform CLI | `winget install Hashicorp.Terraform` |
| AWS CLI | `winget install Amazon.AWSCLI` |

### Path A: Original `docker-compose.yml` (no Floci, no Terraform)

This path works without any new tools. The env vars in docker-compose.yml override the `application.yml` config.

```powershell
# Build and start everything
docker compose -f infra/docker-compose.yml up --build

# Verify
curl http://localhost:8081/actuator/health
curl http://localhost:3000

# Stop
docker compose -f infra/docker-compose.yml down
```

**What it tests**: Applications still work with the classic flow. Spring Cloud AWS is on the classpath but `optional:` import means it doesn't block startup without Floci.

### Set AWS Env Vars (Quick Scripts)

Before running AWS CLI or Terraform, set the Floci endpoint variables:

**Windows (PowerShell):**
```powershell
. .\scripts\set-aws-env.ps1
```

**macOS / Linux (Bash):**
```bash
source scripts/set-aws-env.sh
```

These set `AWS_DEFAULT_REGION=us-east-1`, `AWS_ENDPOINT_URL=http://localhost:4566`, and dummy credentials. The `.` / `source` prefix is required so the variables persist in your current terminal session.

### Path B: `docker-compose-floci.yml` + Terraform (Full Phase 17)

Three-step startup sequence:

**Windows (PowerShell):**
```powershell
# ─── 0. Set AWS env vars ───
. .\scripts\set-aws-env.ps1

# ─── 1. Start Floci ───
docker compose -f infra/docker-compose-floci.yml up -d floci

# Verify Floci is healthy
curl http://localhost:4566/_floci/health

# ─── 2. Provision infrastructure ───
cd infra/terraform
terraform init
terraform apply -var="db_password=localdevpassword" -auto-approve
cd ../..

# ─── 3. Start ALL services ───
docker compose -f infra/docker-compose-floci.yml up --build
```

**macOS / Linux (Bash):**
```bash
# ─── 0. Set AWS env vars ───
source scripts/set-aws-env.sh

# ─── 1. Start Floci ───
docker compose -f infra/docker-compose-floci.yml up -d floci

# Verify
curl http://localhost:4566/_floci/health

# ─── 2. Provision infrastructure ───
cd infra/terraform
terraform init
terraform apply -var="db_password=localdevpassword" -auto-approve
cd ../..

# ─── 3. Start ALL services ───
docker compose -f infra/docker-compose-floci.yml up --build
```

**What it tests**: Full Phase 17 — services fetch credentials from Secrets Manager, infrastructure is provisioned by Terraform against Floci.

---

## Verification Checks

### Health Endpoints

| URL | Service | Expected |
|-----|---------|----------|
| `http://localhost:8081/actuator/health` | Payment Service | `{"status":"UP"}` |
| `http://localhost:8082/actuator/health` | Merchant Service | `{"status":"UP"}` |
| `http://localhost:8083/actuator/health` | Webhook Service | `{"status":"UP"}` |
| `http://localhost:3000` | Frontend | React app loads |
| `http://localhost:8081/payment/swagger-ui.html` | Swagger UI | Interactive docs |
| `http://localhost:9090` | Prometheus | UI loads |
| `http://localhost:3001` | Grafana | Login screen |
| `http://localhost:9411` | Zipkin | Trace search UI |

### Terraform Resources (via AWS CLI)

First, load the Floci endpoint:
- **Windows**: `. .\scripts\set-aws-env.ps1`
- **macOS/Linux**: `source scripts/set-aws-env.sh`

```powershell
# Secrets Manager
aws secretsmanager get-secret-value --secret-id /payflow/local/payment-service/db
aws secretsmanager get-secret-value --secret-id /payflow/local/merchant-service/db
aws secretsmanager get-secret-value --secret-id /payflow/local/webhook-service/db
aws secretsmanager get-secret-value --secret-id /payflow/local/kafka

# ECR Repositories
aws ecr describe-repositories

# KMS Key
aws kms list-keys
```


### No Hardcoded Credentials

Verify that zero hardcoded credentials remain in application source files:

```powershell
# Should return no matches
Select-String -Path "backend/*/src/main/resources/application.yml" `
  -Pattern "spring\.datasource\.(url|username|password): [^$]" `
  | Where-Object { $_ -notmatch '\$\{' }

# Kafka should use env var
Select-String -Path "backend/*/src/main/resources/application.yml" `
  -Pattern "bootstrap-servers" | Select-String -NotMatch "PAYFLOW_KAFKA"
```

---

## Debugging

### Docker Commands

```powershell
# Check running containers
docker compose -f infra/docker-compose-floci.yml ps

# View logs for a specific service
docker compose -f infra/docker-compose-floci.yml logs payment-service

# Follow logs in real-time
docker compose -f infra/docker-compose-floci.yml logs -f payment-service

# Restart a specific service
docker compose -f infra/docker-compose-floci.yml restart merchant-service

# Rebuild and start a single service
docker compose -f infra/docker-compose-floci.yml up --build -d payment-service
```

### Database Checks

When using the original `docker-compose.yml` (standalone Postgres):

```powershell
# Connect to payment DB
docker exec -it payflow-postgres-1 psql -U payflow -d payflow -c "\dn"

# Check tables exist
docker exec -it payflow-postgres-1 psql -U payflow -d payflow -c "\dt payments.*"
```

When using Floci RDS (ports 7001-7003):

```powershell
# Floci RDS instances are real PostgreSQL containers
# Connect to payment RDS on port 7001
psql -h localhost -p 7001 -U payflow -d payflow

# Check Flyway migrations ran
psql -h localhost -p 7001 -U payflow -d payflow -c "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;"
```

### Terraform Debugging

```powershell
# Check what Terraform created
cd infra/terraform
terraform state list

# Show a specific resource
terraform state show module.security.aws_secretsmanager_secret.payment_db

# Plan without applying
terraform plan -var="db_password=localdevpassword"

# Destroy everything
terraform destroy -var="db_password=localdevpassword" -auto-approve
```

### Common Issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| Service won't start, logs show "aws-secretsmanager" error | Floci not running or Terraform not applied | Run steps 1-2 first, or use `docker-compose.yml` without Floci |
| `terraform apply` fails with "error creating MSK cluster" | Floci may not fully support MSK | Check Floci version, try `terraform apply` again — sometimes transient |
| `localhost:8081/actuator/health` returns 503 | Service still starting (JVM + Flyway) | Wait 30-60s, check logs with `docker compose logs payment-service` |
| AWS CLI "NoRegion" error | Region env var not set | Run `. .\scripts\set-aws-env.ps1` (Win) or `source scripts/set-aws-env.sh` (Mac/Linux) |
| Port conflict on 5432, 8081, etc. | Another service using the port | Check `netstat -ano | findstr :PORT` and stop conflicting service |

---

## Secrets Layout

| Secret Path | JSON Content | Used By |
|-------------|-------------|---------|
| `/payflow/{env}/payment-service/db` | `{url, username, password}` | payment-service |
| `/payflow/{env}/merchant-service/db` | `{url, username, password}` | merchant-service |
| `/payflow/{env}/webhook-service/db` | `{url, username, password}` | webhook-service |
| `/payflow/{env}/kafka` | `{bootstrap_servers}` | notification-service |

---

## Files Referenced in This Phase

```
infra/
├── docker-compose.yml            ← UNCHANGED — original quick dev path
├── docker-compose-floci.yml      ← NEW — includes Floci + all services
├── terraform/
│   ├── main.tf                   ← Root module
│   ├── variables.tf
│   ├── outputs.tf
│   ├── terraform.tfvars.local    ← Gitignored
│   └── modules/
│       ├── security/             ← KMS + Secrets Manager + IAM
│       ├── data/                 ← RDS PostgreSQL
│       ├── messaging/            ← MSK / Redpanda
│       └── registry/             ← ECR repositories
└── floci-data/                   ← Floci persistent data (gitignored)

backend/
├── pom.xml                       ← + spring-cloud-aws BOM
├── payment-service/pom.xml       ← + secrets-manager starter
├── merchant-service/pom.xml      ← + secrets-manager starter
├── webhook-service/pom.xml       ← + secrets-manager starter
├── notification-service/pom.xml  ← + secrets-manager starter
├── payment-service/src/main/resources/application.yml  ← Secrets Manager refs
├── merchant-service/src/main/resources/application.yml ← Secrets Manager refs
├── webhook-service/src/main/resources/application.yml  ← Secrets Manager refs
└── notification-service/src/main/resources/application.yml ← Secrets Manager refs

.github/workflows/
└── infra-ci.yml                  ← Terraform fmt + validate on PR

scripts/
├── set-aws-env.ps1               ← PowerShell: load AWS env vars for Floci
└── set-aws-env.sh                ← Bash: load AWS env vars for Floci (macOS/Linux)
```
