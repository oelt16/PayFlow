package com.payflow.payment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class OutboxRelayIntegrationTest extends PaymentIntegrationInfrastructure {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.payflow.payment.infrastructure.kafka.OutboxRelay outboxRelay;

    @Test
    void createPaymentViaApi_publishesPaymentCreatedEnvelopeToKafka() throws Exception {
        MvcResult created = mockMvc.perform(
                        post("/v1/payments")
                                .header("Authorization", "Bearer sk_test_dev")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateBody("USD"))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String paymentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        outboxRelay.publishUnpublishedOutboxEvents();

        ConsumerRecord<String, String> record = pollOne("payments.events", paymentId);
        assertThat(record.key()).isEqualTo("mer_test_dev");

        JsonNode envelope = objectMapper.readTree(record.value());
        assertThat(envelope.get("eventType").asText()).isEqualTo("payment.created");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(paymentId);
        assertThat(envelope.get("merchantId").asText()).isEqualTo("mer_test_dev");
        assertThat(envelope.get("payload").get("status").asText()).isEqualTo("PENDING");
    }

    private ConsumerRecord<String, String> pollOne(String topic, String aggregateIdInPayload) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CONTAINER2_KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-relay-itest-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        long deadline = System.currentTimeMillis() + 60_000;
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            while (System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    if (r.value() != null && r.value().contains("\"aggregateId\":\"" + aggregateIdInPayload + "\"")) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("No Kafka message for aggregate " + aggregateIdInPayload + " within timeout");
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
