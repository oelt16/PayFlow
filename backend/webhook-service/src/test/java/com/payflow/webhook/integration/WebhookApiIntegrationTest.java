package com.payflow.webhook.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.payflow.webhook.application.port.WebhookSendResult;
import com.payflow.webhook.application.port.WebhookSender;
import com.payflow.webhook.infrastructure.scheduling.WebhookDeliveryRetryJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WebhookApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        seedMerchantsTableForTests(POSTGRES);
        String base = POSTGRES.getJdbcUrl();
        String url = base.contains("?") ? base + "&currentSchema=webhooks" : base + "?currentSchema=webhooks";
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "webhooks");
        registry.add("spring.flyway.schemas", () -> "webhooks");
        registry.add("spring.flyway.default-schema", () -> "webhooks");
    }

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
            throw new IllegalStateException("Failed to seed merchants for webhook integration tests", e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private WebhookSender webhookSender;

    @MockBean
    private WebhookDeliveryRetryJob webhookDeliveryRetryJob;

    @Test
    void registerDispatchAndDeliveryRecorded() throws Exception {
        when(webhookSender.send(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(WebhookSendResult.ok(200));

        MvcResult reg = mockMvc.perform(
                        post("/v1/webhooks")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {"url":"https://example.com/hook","events":["payment.created"]}
                                                """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String webhookId = objectMapper.readTree(reg.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        post("/internal/webhooks/dispatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                  "merchantId": "mer_test_dev",
                                                  "eventType": "payment.created",
                                                  "eventPayload": {"hello":"world"}
                                                }
                                                """
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/v1/webhooks/{id}/deliveries", webhookId)
                                .header("Authorization", "Bearer sk_test_dev")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].status").value("DELIVERED"));

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from webhooks.webhook_deliveries where webhook_id = ?",
                Integer.class,
                webhookId
        );
        assertThat(rows).isEqualTo(1);
    }
}
