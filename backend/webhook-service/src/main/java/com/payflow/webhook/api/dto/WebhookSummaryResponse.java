package com.payflow.webhook.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Webhook endpoint summary")
public record WebhookSummaryResponse(
        @Schema(description = "Unique webhook identifier", example = "wh_abc123") String id,
        @Schema(description = "Webhook endpoint URL", example = "https://example.com/webhooks") String url,
        @Schema(description = "Subscribed event types", example = "[\"payment.succeeded\", \"payment.failed\"]") List<String> events,
        @Schema(description = "Whether the webhook is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z") String createdAt
) {
}
