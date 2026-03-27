package com.payflow.payment.domain;

import java.util.Objects;
import java.util.UUID;

public final class PaymentId {

    private final String value;

    private PaymentId(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static PaymentId generate() {
        return new PaymentId("pay_" + UUID.randomUUID().toString().replace("-", ""));
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
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
        PaymentId paymentId = (PaymentId) o;
        return value.equals(paymentId.value);
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
