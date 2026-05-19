package com.payflow.webhook.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for webhook-service.
 * Tracks webhook delivery attempts, successes, and failures.
 */
@Component
public class WebhookServiceMetrics {

    private final Counter deliveryAttempted;
    private final Counter deliverySucceeded;
    private final Counter deliveryFailed;
    private final Counter deliveryRetried;

    public WebhookServiceMetrics(MeterRegistry registry) {
        this.deliveryAttempted = Counter.builder("payflow.webhook.delivery.attempted")
                .register(registry);
        this.deliverySucceeded = Counter.builder("payflow.webhook.delivery.succeeded")
                .register(registry);
        this.deliveryFailed = Counter.builder("payflow.webhook.delivery.failed")
                .register(registry);
        this.deliveryRetried = Counter.builder("payflow.webhook.delivery.retried")
                .register(registry);
    }

    public void recordDeliveryAttempted() {
        deliveryAttempted.increment();
    }

    public void recordDeliverySucceeded() {
        deliverySucceeded.increment();
    }

    public void recordDeliveryFailed() {
        deliveryFailed.increment();
    }

    public void recordDeliveryRetried() {
        deliveryRetried.increment();
    }
}