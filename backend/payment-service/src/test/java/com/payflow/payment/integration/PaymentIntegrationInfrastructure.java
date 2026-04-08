package com.payflow.payment.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Shared PostgreSQL + Kafka for payment-service integration tests.
 * Field names are ordered so JUnit/Testcontainers starts Postgres before Kafka (alphabetical discovery).
 */
public abstract class PaymentIntegrationInfrastructure {

    @Container
    protected static final PostgreSQLContainer<?> CONTAINER1_POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    /** Official {@code apache/kafka} image (KRaft; tag from Docker Hub, same family Testcontainers expects). */
    @Container
    protected static final KafkaContainer CONTAINER2_KAFKA = new KafkaContainer("apache/kafka");

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER1_POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER1_POSTGRES::getUsername);
        registry.add("spring.datasource.password", CONTAINER1_POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", CONTAINER2_KAFKA::getBootstrapServers);
        registry.add("payflow.outbox.poll-interval-ms", () -> "200");
        registry.add("payflow.security.api-keys[0].key", () -> "sk_test_dev");
        registry.add("payflow.security.api-keys[0].merchant-id", () -> "mer_test_dev");
    }
}
