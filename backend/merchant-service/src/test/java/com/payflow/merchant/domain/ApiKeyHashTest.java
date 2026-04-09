package com.payflow.merchant.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyHashTest {

    @Test
    void ofRejectsBlank() {
        assertThatThrownBy(() -> ApiKeyHash.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> ApiKeyHash.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCodeByValue() {
        ApiKeyHash a = ApiKeyHash.of("$2a$10$abcdefghijklmnopqrstuv");
        ApiKeyHash b = ApiKeyHash.of("$2a$10$abcdefghijklmnopqrstuv");
        ApiKeyHash c = ApiKeyHash.of("$2a$10$xxxxxxxxxxxxxxxxxxxxxx");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.equals(a)).isTrue();
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("$2a$10$")).isFalse();
    }
}
