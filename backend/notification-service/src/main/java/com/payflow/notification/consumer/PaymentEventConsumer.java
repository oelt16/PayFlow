package com.payflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.event.PaymentEventEnvelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payments.events", groupId = "notification-service")
    public void listen(String value) {
        processEnvelopeJson(value);
    }

    void processEnvelopeJson(String value) {
        try {
            PaymentEventEnvelope envelope = objectMapper.readValue(value, PaymentEventEnvelope.class);
            log.info(
                    "Payment domain event: type={} aggregateId={} merchantId={} occurredAt={}",
                    envelope.eventType(),
                    envelope.aggregateId(),
                    envelope.merchantId(),
                    envelope.occurredAt()
            );
        } catch (Exception e) {
            log.warn("Malformed payment event envelope: {}", e.getMessage());
        }
    }
}
