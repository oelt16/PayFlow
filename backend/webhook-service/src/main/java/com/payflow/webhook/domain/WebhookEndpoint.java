package com.payflow.webhook.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

public final class WebhookEndpoint {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebhookId id;
    private final MerchantId merchantId;
    private final String url;
    private final String secret;
    private final Set<String> eventTypes;
    private boolean active;
    private final Instant createdAt;

    private WebhookEndpoint(
            WebhookId id,
            MerchantId merchantId,
            String url,
            String secret,
            Set<String> eventTypes,
            boolean active,
            Instant createdAt
    ) {
        this.id = id;
        this.merchantId = merchantId;
        this.url = url;
        this.secret = secret;
        this.eventTypes = Set.copyOf(eventTypes);
        this.active = active;
        this.createdAt = createdAt;
    }

    public static WebhookEndpoint register(MerchantId merchantId, String url, Set<String> eventTypes, Instant now) {
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(eventTypes, "eventTypes");
        Objects.requireNonNull(now, "now");
        String trimmed = url.trim();
        if (!trimmed.toLowerCase().startsWith("https://")) {
            throw new InvalidWebhookUrlException("Webhook URL must use HTTPS");
        }
        if (eventTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one event type is required");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new WebhookEndpoint(WebhookId.generate(), merchantId, trimmed, secret, eventTypes, true, now);
    }

    /**
     * Rehydrates from persistence (no validation of URL beyond non-null).
     */
    public static WebhookEndpoint restore(
            WebhookId id,
            MerchantId merchantId,
            String url,
            String secret,
            Set<String> eventTypes,
            boolean active,
            Instant createdAt
    ) {
        return new WebhookEndpoint(id, merchantId, url, secret, eventTypes, active, createdAt);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean matchesEvent(String eventType) {
        return active && eventTypes.contains(eventType);
    }

    public WebhookId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public String url() {
        return url;
    }

    public String secret() {
        return secret;
    }

    public Set<String> eventTypes() {
        return eventTypes;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
