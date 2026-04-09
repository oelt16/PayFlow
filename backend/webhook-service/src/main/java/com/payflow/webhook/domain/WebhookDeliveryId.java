package com.payflow.webhook.domain;

import java.util.Objects;
import java.util.UUID;

public final class WebhookDeliveryId {

    private final String value;

    private WebhookDeliveryId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static WebhookDeliveryId generate() {
        return new WebhookDeliveryId("whd_" + UUID.randomUUID().toString().replace("-", ""));
    }

    public static WebhookDeliveryId of(String value) {
        return new WebhookDeliveryId(value);
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
        WebhookDeliveryId that = (WebhookDeliveryId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
