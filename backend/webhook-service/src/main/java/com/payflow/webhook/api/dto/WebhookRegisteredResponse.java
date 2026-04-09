package com.payflow.webhook.api.dto;

import java.util.List;

public record WebhookRegisteredResponse(
        String id,
        String url,
        List<String> events,
        String secret,
        String createdAt
) {
}
