package com.payflow.webhook.api.dto;

public record DeliveryResponse(
        String id,
        String eventType,
        String status,
        int attempts,
        String lastAttemptAt,
        String nextRetryAt,
        String lastError,
        String createdAt
) {
}
