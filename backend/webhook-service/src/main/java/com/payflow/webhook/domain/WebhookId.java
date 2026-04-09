package com.payflow.webhook.domain;

import java.util.Objects;
import java.util.UUID;

public final class WebhookId {

    private final String value;

    private WebhookId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static WebhookId generate() {
        return new WebhookId("wh_" + UUID.randomUUID().toString().replace("-", ""));
    }

    public static WebhookId of(String value) {
        return new WebhookId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebhookId webhookId = (WebhookId) o;
        return value.equals(webhookId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
