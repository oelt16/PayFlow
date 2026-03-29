package com.payflow.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIdTest {

    @Test
    void generateStartsWithPayPrefix() {
        assertThat(PaymentId.generate().value()).startsWith("pay_");
    }

    @Test
    void ofPreservesValue() {
        assertThat(PaymentId.of("pay_custom").value()).isEqualTo("pay_custom");
    }

    @Test
    void ofRejectsNull() {
        assertThatThrownBy(() -> PaymentId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsAndHashCodeByValue() {
        PaymentId a = PaymentId.of("pay_1");
        PaymentId b = PaymentId.of("pay_1");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void toStringReturnsValue() {
        assertThat(PaymentId.of("pay_z").toString()).isEqualTo("pay_z");
    }

    @Test
    void equalsNullIsFalse() {
        assertThat(PaymentId.of("pay_a").equals(null)).isFalse();
    }

    @Test
    void equalsDifferentTypeIsFalse() {
        assertThat(PaymentId.of("pay_a").equals("pay_a")).isFalse();
    }
}
