package com.payflow.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundIdTest {

    @Test
    void generateStartsWithRePrefix() {
        assertThat(RefundId.generate().value()).startsWith("re_");
    }

    @Test
    void ofPreservesValue() {
        assertThat(RefundId.of("re_custom").value()).isEqualTo("re_custom");
    }

    @Test
    void ofRejectsNull() {
        assertThatThrownBy(() -> RefundId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsAndHashCodeByValue() {
        RefundId a = RefundId.of("re_1");
        RefundId b = RefundId.of("re_1");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equalsNullIsFalse() {
        assertThat(RefundId.of("re_a").equals(null)).isFalse();
    }

    @Test
    void equalsDifferentTypeIsFalse() {
        assertThat(RefundId.of("re_a").equals("re_a")).isFalse();
    }

    @Test
    void equalsDifferentValueIsFalse() {
        assertThat(RefundId.of("re_a")).isNotEqualTo(RefundId.of("re_b"));
    }

    @Test
    void equalsSameInstanceIsTrue() {
        RefundId r = RefundId.of("re_same");
        assertThat(r.equals(r)).isTrue();
    }
}
