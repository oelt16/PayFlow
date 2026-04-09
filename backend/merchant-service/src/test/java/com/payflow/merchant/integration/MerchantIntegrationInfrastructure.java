package com.payflow.merchant.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Shared PostgreSQL + Kafka for merchant-service integration tests.
 */
public abstract class MerchantIntegrationInfrastructure {

    @Container
    protected static final PostgreSQLContainer<?> CONTAINER1_POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    protected static final KafkaContainer CONTAINER2_KAFKA = new KafkaContainer("apache/kafka");

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER1_POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER1_POSTGRES::getUsername);
        registry.add("spring.datasource.password", CONTAINER1_POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", CONTAINER2_KAFKA::getBootstrapServers);
        registry.add("payflow.outbox.poll-interval-ms", () -> "200");
    }
}
