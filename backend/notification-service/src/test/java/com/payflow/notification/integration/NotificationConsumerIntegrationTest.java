package com.payflow.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.consumer.PaymentEventConsumer;
import com.payflow.notification.event.PaymentEventEnvelope;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class NotificationConsumerIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka");

    @DynamicPropertySource
    static void registerKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @SpyBean
    private PaymentEventConsumer consumer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void consumerReceivesEnvelopeFromKafka() throws Exception {
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                "evt_itest",
                "payment.created",
                "pay_itest",
                "mer_itest",
                Instant.parse("2024-09-01T12:00:00Z"),
                Map.of("status", "PENDING")
        );
        String json = objectMapper.writeValueAsString(envelope);
        kafkaTemplate.send("payments.events", "mer_itest", json).get(30, TimeUnit.SECONDS);

        verify(consumer, timeout(60_000)).listen(anyString());
    }
}
