# Proposal: Phase 17 — Infrastructure as Code with Terraform + Floci

## Intent

Replace hardcoded DB/Kafka credentials with Secrets Manager at runtime. Provision local AWS via Terraform → Floci. Keep `docker-compose.yml`; add `docker-compose-floci.yml`.

## Scope

### In Scope
- Terraform modules: security (KMS + 4 secrets), data (3 RDS), messaging (MSK/Redpanda), registry (5 ECR repos)
- `docker-compose-floci.yml` — all services + Floci
- Spring Cloud AWS Secrets Manager in all 4 services + root POM BOM
- `infra-ci.yml` — `terraform fmt + validate`
- `.gitignore` for Terraform artifacts

### Out of Scope
- K8s/Helm (Phase 18), chaos (Phase 20), security hardening (Phase 21)
- Production VPC, TLS, networking

## Capabilities

### New Capabilities
- `infrastructure-provisioning`: Terraform modules targeting Floci
- `secret-management`: Spring Cloud AWS Secrets Manager — replaces hardcoded DB + Kafka creds
- `local-aws-emulation`: Floci service in docker-compose

### Modified Capabilities
None.

## Approach

1. **Terraform** `infra/terraform/`: AWS provider → `http://floci:4566`. Modules for KMS, Secrets Manager (4 secrets), RDS (3 instances, Floci-managed PG), MSK (Redpanda), ECR (5 repos).
2. **Root POM** adds `spring-cloud-aws-dependencies` BOM. Each service POM adds `secrets-manager` starter.
3. **application.yml** — replace `spring.datasource.*` and `spring.kafka.bootstrap-servers` with `aws-secretsmanager:...` refs. Add `spring.cloud.aws.endpoint`.
4. **docker-compose-floci.yml** — copies all from `docker-compose.yml`, adds Floci with init containers. Standalone postgres/kafka included for compat.
5. **infra-ci.yml** — `terraform fmt -check + init && validate` on PR.

## Affected Areas

| Area | Impact | What |
|------|--------|------|
| `infra/terraform/` | New | 4 modules |
| `infra/docker-compose-floci.yml` | New | Full stack + Floci |
| `.github/workflows/infra-ci.yml` | New | Terraform CI |
| `backend/pom.xml` | Modified | Cloud AWS BOM |
| `backend/*/pom.xml` | Modified | secrets-manager starter |
| `backend/*/application.yml` | Modified | Secrets Manager refs |
| `.gitignore` | Modified | Terraform patterns |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Docker socket mount for Floci RDS | Low | Document in compose |
| 3-step startup (Floci → tf → services) | Med | depends_on + init script |
| Floci image availability | Low | Verified: 50K+ pulls, MIT |

## Rollback Plan

`docker-compose.yml` untouched — old path works always. Git revert all other changes. Flyway unchanged — no schema rollback.

## Dependencies

- `floci/floci:latest` (verified — 50K+ pulls, MIT)
- Terraform CLI >= 1.6, AWS CLI
- Spring Cloud AWS 2023.0.x (compatible with SB 3.3.6)

## Success Criteria

- [ ] `terraform apply` provisions all resources (KMS, 4 secrets, 3 RDS, 1 MSK, 5 ECR)
- [ ] All 4 services start and fetch creds from Secrets Manager — zero hardcoded values
- [ ] `docker compose -f docker-compose-floci.yml up` starts everything + Floci
- [ ] `infra-ci.yml` runs `terraform validate` on PR
