package com.payflow.payment.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
        CONTAINER1_POSTGRES.start();
        seedMerchantsTableForTests(CONTAINER1_POSTGRES);
        registry.add("spring.datasource.url", CONTAINER1_POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER1_POSTGRES::getUsername);
        registry.add("spring.datasource.password", CONTAINER1_POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", CONTAINER2_KAFKA::getBootstrapServers);
        registry.add("payflow.outbox.poll-interval-ms", () -> "200");
    }

    /**
     * Payment-service auth reads {@code merchants.merchants}; seed a row matching {@code sk_test_dev}
     * so existing integration tests keep using that Bearer token.
     */
    private static void seedMerchantsTableForTests(PostgreSQLContainer<?> pg) {
        String hash = new BCryptPasswordEncoder().encode("sk_test_dev");
        try (Connection c = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
                Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS merchants");
            st.execute(
                    """
                            CREATE TABLE IF NOT EXISTS merchants.merchants (
                                id VARCHAR(64) PRIMARY KEY,
                                name VARCHAR(255) NOT NULL,
                                email VARCHAR(320) NOT NULL,
                                key_prefix VARCHAR(16) NOT NULL,
                                key_hash VARCHAR(128) NOT NULL,
                                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deactivated_at TIMESTAMPTZ
                            )
                            """
            );
            try (PreparedStatement ps = c.prepareStatement(
                    """
                            INSERT INTO merchants.merchants (id, name, email, key_prefix, key_hash, is_active)
                            VALUES (?, ?, ?, ?, ?, TRUE)
                            ON CONFLICT (id) DO UPDATE SET
                              name = excluded.name,
                              email = excluded.email,
                              key_prefix = excluded.key_prefix,
                              key_hash = excluded.key_hash,
                              is_active = excluded.is_active
                            """
            )) {
                ps.setString(1, "mer_test_dev");
                ps.setString(2, "Test");
                ps.setString(3, "test@test.com");
                ps.setString(4, "sk_test_");
                ps.setString(5, hash);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to seed merchants for payment integration tests", e);
        }
    }
}
