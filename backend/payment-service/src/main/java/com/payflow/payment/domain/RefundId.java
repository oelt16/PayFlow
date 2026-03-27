package com.payflow.payment.domain;

import java.util.Objects;
import java.util.UUID;

public final class RefundId {

    private final String value;

    private RefundId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static RefundId generate() {
        return new RefundId("re_" + UUID.randomUUID().toString().replace("-", ""));
    }

    public static RefundId of(String value) {
        return new RefundId(value);
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
        RefundId refundId = (RefundId) o;
        return value.equals(refundId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
