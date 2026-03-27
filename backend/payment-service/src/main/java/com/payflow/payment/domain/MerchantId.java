package com.payflow.payment.domain;

import java.util.Objects;
import java.util.UUID;

public final class MerchantId {

    private final String value;

    private MerchantId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("merchantId must not be blank");
        }
        this.value = value;
    }

    public static MerchantId generate() {
        return new MerchantId("mer_" + UUID.randomUUID().toString().replace("-", ""));
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

    @Override
    public String toString() {
        return value;
    }
}
