package com.payflow.notification.event;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

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
