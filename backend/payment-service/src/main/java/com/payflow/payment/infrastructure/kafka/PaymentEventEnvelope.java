package com.payflow.payment.infrastructure.kafka;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Kafka envelope for {@code payments.events}, aligned with PayFlow spec §5.2.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentEventEnvelope(
        String eventId,
        String eventType,
        String aggregateId,
        String merchantId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
