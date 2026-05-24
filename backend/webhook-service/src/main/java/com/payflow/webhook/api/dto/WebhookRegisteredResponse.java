package com.payflow.webhook.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Webhook registration response — includes the signing secret (shown only once)")
public record WebhookRegisteredResponse(
        @Schema(description = "Unique webhook identifier", example = "wh_abc123") String id,
        @Schema(description = "Webhook endpoint URL", example = "https://example.com/webhooks") String url,
        @Schema(description = "Subscribed event types", example = "[\"payment.succeeded\", \"payment.failed\"]") List<String> events,
        @Schema(description = "HMAC signing secret for signature verification", example = "whsec_abc123def456") String secret,
        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z") String createdAt
) {
}
