package com.payflow.payment.infrastructure.metrics;

import com.payflow.payment.infrastructure.persistence.jpa.OutboxEventSpringDataRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Outbox metrics for payment-service.
 * Registers a gauge for pending (unpublished) outbox events.
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(
            MeterRegistry registry,
            OutboxEventSpringDataRepository outboxRepository
    ) {
        Gauge.builder("payflow.outbox.pending", outboxRepository, r -> {
                    try {
                        return r.countByPublishedFalse();
                    } catch (Exception e) {
                        // If repository is not available (e.g., during startup), return 0
                        return 0L;
                    }
                })
                .tag("service", "payment-service")
                .register(registry);
    }
}