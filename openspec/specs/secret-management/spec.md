# Secret Management Specification

## Purpose

Replace hardcoded database credentials and Kafka bootstrap addresses with Spring Cloud AWS Secrets Manager integration. All four backend services read credentials from Secrets Manager at startup.

## Requirements

### Requirement: Root POM BOM

The root `backend/pom.xml` MUST declare `spring-cloud-aws-dependencies` BOM version `2023.0.x` (compatible with Spring Boot 3.3.6).

#### Scenario: BOM present in dependency management

- GIVEN the root `backend/pom.xml`
- WHEN examined
- THEN it contains `spring-cloud-aws-dependencies` in `<dependencyManagement><dependencies>`
- AND the version matches the `2023.0.x` line

### Requirement: Service POM Dependencies

Each service POM (`payment-service`, `merchant-service`, `webhook-service`, `notification-service`) MUST add `spring-cloud-aws-starter-secrets-manager` as a dependency.

#### Scenario: Starter added to each service

- GIVEN each service's `pom.xml`
- WHEN examined
- THEN it contains `spring-cloud-aws-starter-secrets-manager`
- AND it does NOT specify a version (inherited from BOM)

### Requirement: Secrets Manager Configuration

Each service that reads from Secrets Manager MUST configure `spring.config.import`, `spring.cloud.aws.endpoint`, `spring.cloud.aws.region.static`, and `spring.cloud.aws.credentials.*`.

#### Scenario: Configuration present in application.yml

- GIVEN each service's `application.yml`
- WHEN examined
- THEN `spring.config.import` starts with `aws-secretsmanager:/payflow/${ENVIRONMENT:local}/`
- AND `spring.cloud.aws.endpoint` is `http://localhost:4566`
- AND `spring.cloud.aws.region.static` is `us-east-1`
- AND credentials are `access-key: test` and `secret-key: test`

### Requirement: Database Services Read DB Secrets

Payment-service, merchant-service, and webhook-service MUST read `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` from Secrets Manager. Each service reads its own secret path: `/payflow/${ENVIRONMENT:local}/{service-name}/db`.

#### Scenario: DB creds from Secrets Manager

- GIVEN the service starts with Floci and Terraform applied
- WHEN the service initializes its datasource
- THEN the datasource URL, username, and password come from Secrets Manager
- AND no hardcoded database credentials exist in `application.yml`

### Requirement: Notification Service Reads Kafka Secret

The notification-service MUST read `spring.kafka.bootstrap-servers` from Secrets Manager at path `/payflow/${ENVIRONMENT:local}/kafka`.

#### Scenario: Kafka bootstrap from Secrets Manager

- GIVEN the notification-service starts with Floci and Terraform applied
- WHEN the service initializes its Kafka consumer
- THEN `spring.kafka.bootstrap-servers` comes from Secrets Manager
- AND no hardcoded bootstrap address exists in `application.yml`

### Requirement: No Hardcoded Credentials Remain

After migration, zero hardcoded database or Kafka credentials SHALL remain in any `application.yml` or `application.properties` across all backend services.

#### Scenario: Audit finds no hardcoded values

- GIVEN all four service configurations
- WHEN searching for `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, or `spring.kafka.bootstrap-servers`
- THEN none of these keys have literal values
- AND they are only referenced via `aws-secretsmanager:...` imports
