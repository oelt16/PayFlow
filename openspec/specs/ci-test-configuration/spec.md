# CI Test Configuration Specification

## Purpose

Provide test configuration overrides and dynamic property registration that decouple payment-service integration tests from external AWS Secrets Manager, enabling reliable CI execution without local AWS infrastructure.

## Requirements

### Requirement: Test Profile Disables Secrets Manager

The system MUST provide `backend/payment-service/src/test/resources/application-test.yml` that overrides `spring.config.import` to exclude `aws-secretsmanager`.

#### Scenario: Test context loads without Secrets Manager

- GIVEN the `test` profile is active in payment-service
- WHEN the Spring context loads for integration tests
- THEN `spring.config.import` does NOT reference `aws-secretsmanager`
- AND no connection is attempted to Secrets Manager

### Requirement: Dynamic Datasource Properties for Testcontainers

`PaymentIntegrationInfrastructure.java` MUST register `db.url`, `db.username`, and `db.password` via `@DynamicPropertySource`, using the same Testcontainers PostgreSQL values already set for `spring.datasource.*`.

#### Scenario: All datasource properties available in test context

- GIVEN Testcontainers starts a PostgreSQL container
- WHEN `@DynamicPropertySource` registers properties
- THEN `db.url`, `db.username`, `db.password` are populated from the container
- AND `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` remain populated
- AND the Spring context loads without `ApplicationContext` failure
