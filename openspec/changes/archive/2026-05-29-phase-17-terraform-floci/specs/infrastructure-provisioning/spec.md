# Infrastructure Provisioning Specification

## Purpose

Define Terraform modules that provision all local AWS infrastructure via Floci: security (KMS + Secrets Manager), data (RDS PostgreSQL), messaging (MSK/Redpanda), and container registry (ECR). All resources target `http://localhost:4566`.

## Requirements

### Requirement: Terraform Root Module

The system MUST provide a root module at `infra/terraform/main.tf` with an AWS provider configured with `endpoints` pointing to `http://localhost:4566`.

#### Scenario: Terraform apply succeeds

- GIVEN Floci is running on `localhost:4566`
- WHEN `terraform apply` is executed from `infra/terraform/`
- THEN all modules apply successfully
- AND state is stored locally (no remote backend)

### Requirement: Security Module

The system SHALL provide `infra/terraform/security/` that creates one KMS key with alias and four Secrets Manager secrets for `payment-db`, `merchant-db`, `webhook-db`, and `kafka`.

#### Scenario: Secrets provisioned with KMS encryption

- GIVEN the security module is applied
- WHEN terraform completes
- THEN four secrets exist in Secrets Manager
- AND each secret is encrypted with the KMS key
- AND IAM policies grant each service read access to its own secret

### Requirement: Data Module

The system SHALL provide `infra/terraform/data/` that provisions three RDS PostgreSQL instances for payment, merchant, and webhook services via Floci's RDS proxy.

#### Scenario: Three RDS instances created

- GIVEN the data module is applied
- WHEN terraform completes
- THEN three RDS instances exist with `postgres` engine
- AND Floci manages real PostgreSQL containers behind the RDS API

### Requirement: Messaging Module

The system SHALL provide `infra/terraform/messaging/` that provisions one MSK-compatible Kafka cluster (backed by Redpanda) via Floci.

#### Scenario: Kafka cluster provisioned

- GIVEN the messaging module is applied
- WHEN terraform completes
- THEN one MSK cluster exists
- AND `bootstrap_brokers` output contains a reachable address

### Requirement: Registry Module

The system SHALL provide `infra/terraform/registry/` that provisions five ECR repositories: `payment-service`, `merchant-service`, `webhook-service`, `notification-service`, and `frontend`.

#### Scenario: Five ECR repos created

- GIVEN the registry module is applied
- WHEN terraform completes
- THEN five ECR repositories exist
- AND `registry_url` output is populated

### Requirement: Variables and Outputs

The system MUST expose `aws_region`, `environment`, and `db_password` (sensitive) as input variables. The system MUST expose `kms_key_arn`, all four secret ARNs, `bootstrap_brokers`, and `registry_url` as outputs.

#### Scenario: Variables accepted and outputs returned

- GIVEN terraform is invoked with `-var="environment=local"` and `-var="db_password=..."` plus `TF_VAR_aws_region`
- WHEN apply completes
- THEN all output values are non-empty

### Requirement: CI Workflow

The system SHALL include `.github/workflows/infra-ci.yml` that runs `terraform fmt -check`, `terraform init -backend=false`, and `terraform validate` on every PR.

#### Scenario: PR validation passes

- GIVEN a PR modifies files under `infra/terraform/`
- WHEN the PR is opened or updated
- THEN the CI workflow runs `fmt -check`, `init -backend=false`, `validate`
- AND all three steps pass

### Requirement: Gitignore

The system MUST add Terraform patterns to `.gitignore`: `*.tfstate`, `*.tfstate.*`, `.terraform/`, `crash.log`, `override.tf`, `terraform.tfvars.local`.

#### Scenario: Terraform artifacts excluded

- GIVEN `.gitignore` contains the Terraform patterns
- WHEN `git add infra/terraform/` is executed
- THEN no `.tfstate` or `.terraform/` files are staged
