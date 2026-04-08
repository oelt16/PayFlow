package com.payflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payflow.notification.event.PaymentEventEnvelope;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class PaymentEventConsumerTest {

    private ObjectMapper objectMapper;
    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new PaymentEventConsumer(objectMapper);
    }

    @Test
    void processEnvelopeJson_acceptsKnownEventTypes() throws Exception {
        for (String type : new String[] {
                "payment.created", "payment.captured", "payment.cancelled", "payment.refunded", "payment.expired"
        }) {
            String json = objectMapper.writeValueAsString(sampleEnvelope(type));
            assertThatCode(() -> consumer.processEnvelopeJson(json)).doesNotThrowAnyException();
        }
    }

    @Test
    void processEnvelopeJson_malformedJson_doesNotThrow() {
        assertThatCode(() -> consumer.processEnvelopeJson("not-json")).doesNotThrowAnyException();
    }

    private static PaymentEventEnvelope sampleEnvelope(String eventType) {
        return new PaymentEventEnvelope(
                "evt_test",
                eventType,
                "pay_test",
                "mer_test",
                Instant.parse("2024-09-01T12:00:00Z"),
                Map.of("status", "PENDING")
        );
    }
}
