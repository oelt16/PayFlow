package com.payflow.webhook.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Webhook delivery attempt details")
public record DeliveryResponse(
        @Schema(description = "Unique delivery identifier", example = "del_abc123") String id,
        @Schema(description = "Event type that triggered this delivery", example = "payment.succeeded") String eventType,
        @Schema(description = "Delivery status", example = "delivered", allowableValues = {"pending", "delivered", "failed", "retrying"}) String status,
        @Schema(description = "Number of delivery attempts", example = "1") int attempts,
        @Schema(description = "Timestamp of last attempt", example = "2024-01-01T00:00:00Z") String lastAttemptAt,
        @Schema(description = "Timestamp of next retry, if scheduled", example = "2024-01-01T00:05:00Z") String nextRetryAt,
        @Schema(description = "Last error message, if delivery failed", example = "Connection timeout") String lastError,
        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z") String createdAt
) {
}
