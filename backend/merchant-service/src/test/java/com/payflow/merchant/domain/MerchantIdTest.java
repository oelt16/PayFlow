package com.payflow.merchant.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantIdTest {

    @Test
    void generateStartsWithMerPrefix() {
        MerchantId id = MerchantId.generate();
        assertThat(id.value()).startsWith("mer_");
        assertThat(id.value()).hasSizeGreaterThan(4);
    }

    @Test
    void toStringReturnsRawId() {
        assertThat(MerchantId.of("mer_custom").toString()).isEqualTo("mer_custom");
    }

    @Test
    void ofRejectsBlank() {
        assertThatThrownBy(() -> MerchantId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> MerchantId.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> MerchantId.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCodeByValue() {
        MerchantId a = MerchantId.of("mer_abc");
        MerchantId b = MerchantId.of("mer_abc");
        MerchantId c = MerchantId.of("mer_xyz");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.equals(a)).isTrue();
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("mer_abc")).isFalse();
    }
}
