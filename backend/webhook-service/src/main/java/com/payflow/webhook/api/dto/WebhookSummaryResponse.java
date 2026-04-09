package com.payflow.webhook.api.dto;

import java.util.List;

public record WebhookSummaryResponse(
        String id,
        String url,
        List<String> events,
        boolean active,
        String createdAt
) {
}
