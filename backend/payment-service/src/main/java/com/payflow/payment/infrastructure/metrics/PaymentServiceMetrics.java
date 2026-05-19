package com.payflow.payment.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Micrometer metrics for payment-service.
 * Registers counters, summaries, and timers for payment operations.
 */
@Component
public class PaymentServiceMetrics {

    private final MeterRegistry registry;

    // Counters
    private final Counter paymentCreatedUsd;
    private final Counter paymentCreatedEur;
    private final Counter paymentCreatedGbp;
    private final Counter paymentCaptured;
    private final Counter paymentCancelled;
    private final Counter paymentRefunded;
    private final Counter paymentExpired;
    private final Counter paymentFailedInvalidState;
    private final Counter paymentFailedInsufficientFunds;

    // Distribution summary for payment amounts
    private final DistributionSummary paymentAmountUsd;
    private final DistributionSummary paymentAmountEur;
    private final DistributionSummary paymentAmountGbp;

    // Timers
    private final Timer paymentCaptureLatency;

    public PaymentServiceMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Payment created counters by currency
        this.paymentCreatedUsd = Counter.builder("payflow.payment.created")
                .tag("currency", "USD")
                .register(registry);
        this.paymentCreatedEur = Counter.builder("payflow.payment.created")
                .tag("currency", "EUR")
                .register(registry);
        this.paymentCreatedGbp = Counter.builder("payflow.payment.created")
                .tag("currency", "GBP")
                .register(registry);

        // Other counters
        this.paymentCaptured = Counter.builder("payflow.payment.captured")
                .register(registry);
        this.paymentCancelled = Counter.builder("payflow.payment.cancelled")
                .register(registry);
        this.paymentRefunded = Counter.builder("payflow.payment.refunded")
                .register(registry);
        this.paymentExpired = Counter.builder("payflow.payment.expired")
                .register(registry);

        // Failed counters by reason
        this.paymentFailedInvalidState = Counter.builder("payflow.payment.failed")
                .tag("reason", "invalid_state")
                .register(registry);
        this.paymentFailedInsufficientFunds = Counter.builder("payflow.payment.failed")
                .tag("reason", "insufficient_funds")
                .register(registry);

        // Distribution summaries for payment amounts (in cents)
        this.paymentAmountUsd = DistributionSummary.builder("payflow.payment.amount")
                .tag("currency", "USD")
                .baseUnit("cents")
                .register(registry);
        this.paymentAmountEur = DistributionSummary.builder("payflow.payment.amount")
                .tag("currency", "EUR")
                .baseUnit("cents")
                .register(registry);
        this.paymentAmountGbp = DistributionSummary.builder("payflow.payment.amount")
                .tag("currency", "GBP")
                .baseUnit("cents")
                .register(registry);

        // Timer for capture latency
        this.paymentCaptureLatency = Timer.builder("payflow.payment.capture.latency")
                .register(registry);
    }

    public void recordPaymentCreated(String currency, long amountCents) {
        switch (currency) {
            case "USD" -> paymentCreatedUsd.increment();
            case "EUR" -> paymentCreatedEur.increment();
            case "GBP" -> paymentCreatedGbp.increment();
            default -> {
                // Log unknown currency but don't crash
            }
        }
        recordAmount(currency, amountCents);
    }

    public void recordPaymentCaptured() {
        paymentCaptured.increment();
    }

    public void recordPaymentCancelled() {
        paymentCancelled.increment();
    }

    public void recordPaymentRefunded() {
        paymentRefunded.increment();
    }

    public void recordPaymentExpired() {
        paymentExpired.increment();
    }

    public void recordPaymentFailed(String reason) {
        switch (reason) {
            case "invalid_state" -> paymentFailedInvalidState.increment();
            case "insufficient_funds" -> paymentFailedInsufficientFunds.increment();
            default -> {
                // Unknown reason
            }
        }
    }

    private void recordAmount(String currency, long amountCents) {
        switch (currency) {
            case "USD" -> paymentAmountUsd.record(amountCents);
            case "EUR" -> paymentAmountEur.record(amountCents);
            case "GBP" -> paymentAmountGbp.record(amountCents);
            default -> {
                // Unknown currency
            }
        }
    }

    public Timer.Sample startCaptureTimer() {
        return Timer.start(registry);
    }

    public void stopCaptureTimer(Timer.Sample sample) {
        sample.stop(paymentCaptureLatency);
    }
}