package com.payflow.webhook.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class WebhookDelivery {

    public static final int MAX_ATTEMPTS = 5;

    private final WebhookDeliveryId id;
    private final WebhookId webhookId;
    private final String eventType;
    private final String eventPayloadJson;
    private DeliveryStatus status;
    private int attempts;
    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private String lastError;
    private final Instant createdAt;

    private WebhookDelivery(
            WebhookDeliveryId id,
            WebhookId webhookId,
            String eventType,
            String eventPayloadJson,
            DeliveryStatus status,
            int attempts,
            Instant lastAttemptAt,
            Instant nextRetryAt,
            String lastError,
            Instant createdAt
    ) {
        this.id = id;
        this.webhookId = webhookId;
        this.eventType = eventType;
        this.eventPayloadJson = eventPayloadJson;
        this.status = status;
        this.attempts = attempts;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
    }

    public static WebhookDelivery createPending(
            WebhookId webhookId,
            String eventType,
            String eventPayloadJson,
            Instant now
    ) {
        Objects.requireNonNull(webhookId, "webhookId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(eventPayloadJson, "eventPayloadJson");
        Objects.requireNonNull(now, "now");
        return new WebhookDelivery(
                WebhookDeliveryId.generate(),
                webhookId,
                eventType,
                eventPayloadJson,
                DeliveryStatus.PENDING,
                0,
                null,
                null,
                null,
                now
        );
    }

    public static WebhookDelivery restore(
            WebhookDeliveryId id,
            WebhookId webhookId,
            String eventType,
            String eventPayloadJson,
            DeliveryStatus status,
            int attempts,
            Instant lastAttemptAt,
            Instant nextRetryAt,
            String lastError,
            Instant createdAt
    ) {
        return new WebhookDelivery(
                id,
                webhookId,
                eventType,
                eventPayloadJson,
                status,
                attempts,
                lastAttemptAt,
                nextRetryAt,
                lastError,
                createdAt
        );
    }

    public void recordSuccess(Instant now) {
        Objects.requireNonNull(now, "now");
        this.status = DeliveryStatus.DELIVERED;
        this.lastAttemptAt = now;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void recordFailure(String error, Instant now) {
        Objects.requireNonNull(now, "now");
        this.attempts++;
        this.lastAttemptAt = now;
        this.lastError = error != null ? error : "unknown error";
        if (this.attempts >= MAX_ATTEMPTS) {
            this.status = DeliveryStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            this.status = DeliveryStatus.PENDING;
            this.nextRetryAt = now.plus(backoffAfterAttempt(this.attempts));
        }
    }

    /**
     * Delay before the next attempt after the current failure ({@code attempts} is count so far).
     */
    static Duration backoffAfterAttempt(int attemptsAfterIncrement) {
        return switch (attemptsAfterIncrement) {
            case 1 -> Duration.ofSeconds(5);
            case 2 -> Duration.ofSeconds(30);
            case 3 -> Duration.ofMinutes(2);
            case 4 -> Duration.ofMinutes(10);
            default -> Duration.ofHours(1);
        };
    }

    public boolean isDue(Instant now) {
        if (status != DeliveryStatus.PENDING) {
            return false;
        }
        if (nextRetryAt == null) {
            return true;
        }
        return !nextRetryAt.isAfter(now);
    }

    public WebhookDeliveryId id() {
        return id;
    }

    public WebhookId webhookId() {
        return webhookId;
    }

    public String eventType() {
        return eventType;
    }

    public String eventPayloadJson() {
        return eventPayloadJson;
    }

    public DeliveryStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Instant lastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant nextRetryAt() {
        return nextRetryAt;
    }

    public String lastError() {
        return lastError;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
