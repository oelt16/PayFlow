package com.payflow.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantIdTest {

    @Test
    void ofRejectsNull() {
        assertThatThrownBy(() -> MerchantId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRejectsBlank() {
        assertThatThrownBy(() -> MerchantId.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofAcceptsNonBlank() {
        assertThat(MerchantId.of("mer_abc").value()).isEqualTo("mer_abc");
    }

    @Test
    void generateStartsWithMerPrefix() {
        assertThat(MerchantId.generate().value()).startsWith("mer_");
    }

    @Test
    void equalsAndHashCodeByValue() {
        MerchantId a = MerchantId.of("mer_x");
        MerchantId b = MerchantId.of("mer_x");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void toStringReturnsValue() {
        assertThat(MerchantId.of("mer_z").toString()).isEqualTo("mer_z");
    }

    @Test
    void equalsNullIsFalse() {
        assertThat(MerchantId.of("mer_a").equals(null)).isFalse();
    }

    @Test
    void equalsDifferentTypeIsFalse() {
        assertThat(MerchantId.of("mer_a").equals("mer_a")).isFalse();
    }
}
