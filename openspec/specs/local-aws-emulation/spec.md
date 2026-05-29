# Local AWS Emulation Specification

## Purpose

Provide a Docker Compose file (`infra/docker-compose-floci.yml`) that includes all existing services plus the Floci AWS emulator. Floci replaces real AWS with local equivalents for KMS, Secrets Manager, RDS, MSK, and ECR.

## Requirements

### Requirement: Docker Compose File

The system MUST provide `infra/docker-compose-floci.yml` containing ALL services from the existing `docker-compose.yml` PLUS a Floci service. The original `docker-compose.yml` MUST remain unchanged.

#### Scenario: Full stack composability

- GIVEN `infra/docker-compose-floci.yml`
- WHEN compared to `docker-compose.yml`
- THEN all services from the original are present in the new file
- AND the original file has zero modifications

### Requirement: Floci Service Configuration

The Floci service MUST use image `floci/floci:latest`, expose port `4566` and RDS proxy range `7001-7003`, mount `/var/run/docker.sock` and `./floci-data:/app/data`, and configure `FLOCI_STORAGE_MODE=hybrid`, `FLOCI_SERVICES_RDS_PROXY_BASE_PORT=7001`, and `FLOCI_SERVICES_DOCKER_NETWORK=infra_default`. A healthcheck on `/_floci/health` MUST be present.

#### Scenario: Floci healthcheck passes

- GIVEN `docker compose -f infra/docker-compose-floci.yml up -d floci`
- WHEN the Floci container starts
- THEN the healthcheck at `/_floci/health` returns HTTP 200
- AND port `4566` is accepting AWS API calls

### Requirement: Service Dependencies on Floci

All services (postgres, kafka, application services) MUST depend on Floci being healthy before starting.

#### Scenario: Services start after Floci

- GIVEN Floci is healthy
- WHEN all services are started
- THEN each service's `depends_on` condition references Floci's health
- AND no service attempts connection before Floci is ready

### Requirement: Startup Sequence

The startup sequence MUST be: (1) Floci container, (2) Terraform apply to provision resources, (3) remaining services with application containers.

#### Scenario: Three-step startup succeeds

- GIVEN `docker compose -f infra/docker-compose-floci.yml up -d floci` succeeds
- WHEN `terraform apply` runs from `infra/terraform/`
- THEN all KMS, secrets, RDS, MSK, and ECR resources are created
- AND the final `docker compose -f infra/docker-compose-floci.yml up --build` starts all services
- AND each service connects to Floci-provisioned resources

### Requirement: Original Docker Compose Preservation

The original `docker-compose.yml` MUST continue to function as a standalone startup path without Floci. This is the rollback path.

#### Scenario: Original compose still works

- GIVEN no Floci or Terraform
- WHEN `docker compose up` with the original `docker-compose.yml`
- THEN all services start
- AND they use hardcoded credentials (unchanged)
