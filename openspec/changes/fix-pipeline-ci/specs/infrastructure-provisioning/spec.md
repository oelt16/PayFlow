# Delta for Infrastructure Provisioning

## MODIFIED Requirements

### Requirement: CI Workflow

The system SHALL include `.github/workflows/infra-ci.yml` that runs `terraform fmt -check`, `terraform init -backend=false`, and `terraform validate` on every PUSH (any branch) and every PR.
(Previously: Only triggered on PR to main/master)

#### Scenario: PR validation passes

- GIVEN a PR modifies files under `infra/terraform/`
- WHEN the PR is opened or updated
- THEN the CI workflow runs `fmt -check`, `init -backend=false`, `validate`
- AND all three steps pass

#### Scenario: Push triggers validation on feature branches

- GIVEN a push modifies files under `infra/terraform/` on any branch
- WHEN the push completes
- THEN `infra-ci.yml` runs `fmt -check`, `init -backend=false`, `validate`
- AND all three steps pass

## ADDED Requirements

### Requirement: CI Push Triggers for Backend and Frontend

The system MUST add `push:` triggers (no branch filter) to `.github/workflows/backend-ci.yml` and `.github/workflows/frontend-ci.yml`, preserving existing `paths` and `pull_request` triggers. Docker build jobs SHALL continue to gate on `main`/`master` only.

#### Scenario: Push triggers backend CI

- GIVEN a push modifies files matching `backend-ci.yml`'s paths filter on any branch
- WHEN the push completes
- THEN `backend-ci.yml` runs unit and integration tests
- AND the Docker build job is skipped on non-main branches

#### Scenario: Push triggers frontend CI

- GIVEN a push modifies files matching `frontend-ci.yml`'s paths filter on any branch
- WHEN the push completes
- THEN `frontend-ci.yml` runs lint and unit tests
- AND the Docker build job is skipped on non-main branches
