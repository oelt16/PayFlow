package com.payflow.webhook.domain;

import java.util.Objects;

public final class MerchantId {

    private final String value;

    private MerchantId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
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
        MerchantId that = (MerchantId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
