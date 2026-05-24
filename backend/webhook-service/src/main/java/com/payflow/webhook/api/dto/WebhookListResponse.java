package com.payflow.webhook.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "List of registered webhook endpoints")
public record WebhookListResponse(
        @Schema(description = "Webhook endpoint items") List<WebhookSummaryResponse> content
) {
}
