package com.payflow.merchant.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MerchantEventEnvelope(
        String eventId,
        String eventType,
        String aggregateId,
        String merchantId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
