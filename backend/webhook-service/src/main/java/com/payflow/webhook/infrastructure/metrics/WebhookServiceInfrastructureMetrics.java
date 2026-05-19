package com.payflow.webhook.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Webhook service infrastructure metrics.
 * Note: webhook-service does not use outbox pattern, so we track metrics differently.
 */
@Component
public class WebhookServiceInfrastructureMetrics {

    // Placeholder for future outbox pattern implementation
    // Currently webhook-service uses direct HTTP dispatch without outbox

    public WebhookServiceInfrastructureMetrics(MeterRegistry registry) {
        // No-op: will be implemented when/if webhook-service adopts outbox pattern
        // The payment-service already has working outbox metrics
    }
}