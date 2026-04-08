package com.payflow.payment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class PaymentApiIntegrationTest extends PaymentIntegrationInfrastructure {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.payflow.payment.infrastructure.kafka.OutboxRelay outboxRelay;

    @Test
    void postPaymentWithoutAuthReturns401() throws Exception {
        mockMvc.perform(
                        post("/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_api_key"));
    }

    @Test
    void postPaymentReturns201Pending() throws Exception {
        mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.amount").value(10_000));
    }

    @Test
    void postPaymentWithInvalidCurrencyReturns400() throws Exception {
        mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("XXX"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("invalid_currency"));
    }

    @Test
    void captureTwiceIsIdempotentAndWritesSingleCapturedOutboxEvent() throws Exception {
        MvcResult created = mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(created.getResponse().getContentAsString());
        String paymentId = root.get("id").asText();

        mockMvc.perform(
                        post("/v1/payments/{id}/capture", paymentId)
                                .header("Authorization", "Bearer sk_test_dev")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        mockMvc.perform(
                        post("/v1/payments/{id}/capture", paymentId)
                                .header("Authorization", "Bearer sk_test_dev")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        Integer capturedEvents = jdbcTemplate.queryForObject(
                "select count(*) from payments.outbox_events where aggregate_id = ? and event_type = 'payment.captured'",
                Integer.class,
                paymentId
        );
        assertThat(capturedEvents).isEqualTo(1);
    }

    @Test
    void listPaymentsFiltersByStatus() throws Exception {
        MvcResult created = mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(
                        post("/v1/payments/{id}/capture", paymentId)
                                .header("Authorization", "Bearer sk_test_dev")
                )
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(
                        get("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .param("status", "CAPTURED")
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listRoot = objectMapper.readTree(listed.getResponse().getContentAsString());
        assertThat(listRoot.get("content").size()).isGreaterThanOrEqualTo(1);
        boolean found = false;
        for (JsonNode item : listRoot.get("content")) {
            assertThat(item.get("status").asText()).isEqualTo("CAPTURED");
            if (paymentId.equals(item.get("id").asText())) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void postPaymentPublishesPaymentCreatedToKafka() throws Exception {
        MvcResult created = mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        outboxRelay.publishUnpublishedOutboxEvents();

        ConsumerRecord<String, String> record = pollPaymentCreatedRecord(paymentId);
        assertThat(record.key()).isEqualTo("mer_test_dev");
        JsonNode envelope = objectMapper.readTree(record.value());
        assertThat(envelope.get("eventType").asText()).isEqualTo("payment.created");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(paymentId);
    }

    private ConsumerRecord<String, String> pollPaymentCreatedRecord(String paymentId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CONTAINER2_KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-api-itest-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        long deadline = System.currentTimeMillis() + 60_000;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("payments.events"));
            while (System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    if (r.value() != null
                            && r.value().contains("\"eventType\":\"payment.created\"")
                            && r.value().contains("\"aggregateId\":\"" + paymentId + "\"")) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("No payment.created Kafka message for " + paymentId);
    }

    private static String validCreateBody(String currency) {
        return """
                {
                  "amount": 10000,
                  "currency": "%s",
                  "description": "Order #1042",
                  "card": {
                    "number": "4242424242424242",
                    "expMonth": 12,
                    "expYear": 2027,
                    "cvc": "123"
                  },
                  "metadata": { "orderId": "ORD-789" }
                }
                """.formatted(currency);
    }
}
