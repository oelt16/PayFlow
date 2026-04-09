package com.payflow.merchant.integration;

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
class OutboxRelayIntegrationTest extends MerchantIntegrationInfrastructure {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.payflow.merchant.infrastructure.kafka.OutboxRelay outboxRelay;

    @Test
    void registerMerchant_publishesMerchantCreatedEnvelopeToKafka() throws Exception {
        MvcResult created = mockMvc.perform(
                        post("/v1/merchants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"KafkaCo\",\"email\":\"kafka@example.com\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String merchantId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        outboxRelay.publishUnpublishedOutboxEvents();

        ConsumerRecord<String, String> record = pollOne("merchant.events", merchantId);
        assertThat(record.key()).isEqualTo(merchantId);

        JsonNode envelope = objectMapper.readTree(record.value());
        assertThat(envelope.get("eventType").asText()).isEqualTo("merchant.created");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(merchantId);
        assertThat(envelope.get("merchantId").asText()).isEqualTo(merchantId);
        assertThat(envelope.get("payload").get("email").asText()).isEqualTo("kafka@example.com");
    }

    private ConsumerRecord<String, String> pollOne(String topic, String aggregateId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CONTAINER2_KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "merchant-outbox-itest-" + System.nanoTime());
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
                    if (r.value() != null && r.value().contains("\"aggregateId\":\"" + aggregateId + "\"")) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("No Kafka message for aggregate " + aggregateId + " within timeout");
    }
}
