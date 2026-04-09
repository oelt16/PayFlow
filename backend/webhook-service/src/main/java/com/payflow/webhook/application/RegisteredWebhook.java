package com.payflow.webhook.application;

import com.payflow.webhook.domain.WebhookId;

import java.time.Instant;
import java.util.Set;

public record RegisteredWebhook(
        WebhookId id,
        String url,
        Set<String> eventTypes,
        String secret,
        Instant createdAt
) {
}
