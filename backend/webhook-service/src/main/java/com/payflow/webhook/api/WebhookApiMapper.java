package com.payflow.webhook.api;

import com.payflow.webhook.api.dto.DeliveryResponse;
import com.payflow.webhook.api.dto.WebhookRegisteredResponse;
import com.payflow.webhook.api.dto.WebhookSummaryResponse;
import com.payflow.webhook.application.RegisteredWebhook;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;

import java.util.List;

public final class WebhookApiMapper {

    private WebhookApiMapper() {
    }

    public static WebhookRegisteredResponse toRegistered(RegisteredWebhook w) {
        return new WebhookRegisteredResponse(
                w.id().value(),
                w.url(),
                List.copyOf(w.eventTypes()),
                w.secret(),
                w.createdAt().toString()
        );
    }

    public static WebhookSummaryResponse toSummary(WebhookEndpoint e) {
        return new WebhookSummaryResponse(
                e.id().value(),
                e.url(),
                List.copyOf(e.eventTypes()),
                e.active(),
                e.createdAt().toString()
        );
    }

    public static DeliveryResponse toDelivery(WebhookDelivery d) {
        return new DeliveryResponse(
                d.id().value(),
                d.eventType(),
                d.status().name(),
                d.attempts(),
                d.lastAttemptAt() != null ? d.lastAttemptAt().toString() : null,
                d.nextRetryAt() != null ? d.nextRetryAt().toString() : null,
                d.lastError(),
                d.createdAt().toString()
        );
    }
}
