package com.payflow.payment.integration;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Payment Expiry Scheduler.
 * Verifies that pending payments older than 1 hour are expired and generate events.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentExpirySchedulerTest extends PaymentIntegrationInfrastructure {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.payflow.payment.infrastructure.scheduler.PaymentExpiryScheduler scheduler;

    /**
     * Test that scheduler expires pending payments older than 1 hour and creates outbox events.
     * Query: status=PENDING AND createdAt<(now-1h) AND expiresAt<=now
     */
    @Test
    void schedulerExpiresOldPendingPaymentsAndCreatesOutboxEvent() throws Exception {
        // Create a payment via API to get a valid payment
        String createBody = """
            {
              "amount": 5000,
              "currency": "USD",
              "description": "Test payment for expiry",
              "card": {
                "number": "4242424242424242",
                "expMonth": 12,
                "expYear": 2027,
                "cvc": "123"
              },
              "metadata": {"orderId": "ORD-EXPIRY-001"}
            }
            """;

        var createdResult = mockMvc.perform(
                post("/v1/payments")
                        .header("Authorization", "Bearer sk_test_dev")
                        .contentType("application/json")
                        .content(createBody)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String paymentId = createdResult.getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];

        // Manually backdate the payment to make it old enough for expiry
        // Set created_at to 2 hours ago and expires_at to 1 hour ago
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));

        jdbcTemplate.update("""
            UPDATE payments.payments
            SET created_at = ?, expires_at = ?
            WHERE id = ?
            """, twoHoursAgo, oneHourAgo, paymentId);

        // Verify initial state
        String initialStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments.payments WHERE id = ?",
                String.class, paymentId);
        assertThat(initialStatus).isEqualTo("PENDING");

        // Run the scheduler - it should expire this payment
        scheduler.runExpiryJob();

        // Verify payment is now EXPIRED
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments.payments WHERE id = ?",
                String.class, paymentId);
        assertThat(finalStatus).isEqualTo("EXPIRED");

        // Verify outbox event was created for payment.expired
        Integer eventCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM outbox.outbox_events
                WHERE aggregate_id = ? AND event_type = ?
                """,
                Integer.class, paymentId, "payment.expired");

        assertThat(eventCount).isGreaterThan(0);
    }

    /**
     * Test that scheduler does NOT expire recent payments (created within last hour).
     */
    @Test
    void schedulerDoesNotExpireRecentPayments() throws Exception {
        // Create a payment
        String createBody = """
            {
              "amount": 3000,
              "currency": "EUR",
              "description": "Recent payment",
              "card": {
                "number": "4242424242424242",
                "expMonth": 12,
                "expYear": 2027,
                "cvc": "123"
              },
              "metadata": {}
            }
            """;

        var createdResult = mockMvc.perform(
                post("/v1/payments")
                        .header("Authorization", "Bearer sk_test_dev")
                        .contentType("application/json")
                        .content(createBody)
        )
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = createdResult.getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];

        // Set created_at to 30 minutes ago but expires_at in the future
        Instant thirtyMinutesAgo = Instant.now().minus(Duration.ofMinutes(30));
        Instant inOneHour = Instant.now().plus(Duration.ofHours(1));

        jdbcTemplate.update("""
            UPDATE payments.payments
            SET created_at = ?, expires_at = ?
            WHERE id = ?
            """, thirtyMinutesAgo, inOneHour, paymentId);

        // Run the scheduler
        scheduler.runExpiryJob();

        // Verify payment is still PENDING
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments.payments WHERE id = ?",
                String.class, paymentId);
        assertThat(finalStatus).isEqualTo("PENDING");
    }

    /**
     * Test that scheduler does NOT expire payments that have already expired (expiresAt > now but status is not EXPIRED).
     * This tests edge case - payment with expiresAt in the future should not be expired.
     */
    @Test
    void schedulerDoesNotExpireFuturePayments() throws Exception {
        // Create a payment
        String createBody = """
            {
              "amount": 2000,
              "currency": "GBP",
              "description": "Future payment",
              "card": {
                "number": "4242424242424242",
                "expMonth": 12,
                "expYear": 2027,
                "cvc": "123"
              },
              "metadata": {}
            }
            """;

        var createdResult = mockMvc.perform(
                post("/v1/payments")
                        .header("Authorization", "Bearer sk_test_dev")
                        .contentType("application/json")
                        .content(createBody)
        )
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = createdResult.getResponse().getContentAsString().split("\"id\":\"")[1].split("\"")[0];

        // Set created_at to 2 hours ago but expires_at in 30 minutes
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        Instant in30Minutes = Instant.now().plus(Duration.ofMinutes(30));

        jdbcTemplate.update("""
            UPDATE payments.payments
            SET created_at = ?, expires_at = ?
            WHERE id = ?
            """, twoHoursAgo, in30Minutes, paymentId);

        // Run the scheduler - should NOT expire because expiresAt > now
        scheduler.runExpiryJob();

        // Verify payment is still PENDING
        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments.payments WHERE id = ?",
                String.class, paymentId);
        assertThat(finalStatus).isEqualTo("PENDING");
    }
}