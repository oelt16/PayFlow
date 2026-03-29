package com.payflow.payment.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardDetailsTest {

    @Test
    void validCardDetails() {
        CardDetails c = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        assertThat(c.last4()).isEqualTo("4242");
        assertThat(c.brand()).isEqualTo(CardBrand.VISA);
        assertThat(c.expiryMonth()).isEqualTo(12);
        assertThat(c.expiryYear()).isEqualTo(2027);
    }

    @ParameterizedTest
    @ValueSource(strings = {"424", "42424", "abcd", "424a"})
    void last4MustBeExactlyFourDigits(String bad) {
        assertThatThrownBy(() -> new CardDetails(bad, CardBrand.MASTERCARD, 6, 2030))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiryMonthOutOfRangeLow() {
        assertThatThrownBy(() -> new CardDetails("1234", CardBrand.AMEX, 0, 2026))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiryMonthOutOfRangeHigh() {
        assertThatThrownBy(() -> new CardDetails("1234", CardBrand.AMEX, 13, 2026))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiryYearOutOfRange() {
        assertThatThrownBy(() -> new CardDetails("1234", CardBrand.VISA, 1, 1999))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardDetails("1234", CardBrand.VISA, 1, 10000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullLast4() {
        assertThatThrownBy(() -> new CardDetails(null, CardBrand.VISA, 1, 2028))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullBrand() {
        assertThatThrownBy(() -> new CardDetails("4242", null, 1, 2028))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsSameFields() {
        CardDetails a = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        CardDetails b = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equalsNullIsFalse() {
        CardDetails c = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        assertThat(c.equals(null)).isFalse();
    }

    @Test
    void equalsDifferentTypeIsFalse() {
        CardDetails c = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        assertThat(c.equals("4242")).isFalse();
    }

    @Test
    void equalsDifferentLast4IsFalse() {
        CardDetails a = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        CardDetails b = new CardDetails("4243", CardBrand.VISA, 12, 2027);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsSameInstanceIsTrue() {
        CardDetails c = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        assertThat(c.equals(c)).isTrue();
    }

    @Test
    void equalsDifferentBrandIsFalse() {
        CardDetails a = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        CardDetails b = new CardDetails("4242", CardBrand.MASTERCARD, 12, 2027);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsDifferentExpiryMonthIsFalse() {
        CardDetails a = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        CardDetails b = new CardDetails("4242", CardBrand.VISA, 11, 2027);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsDifferentExpiryYearIsFalse() {
        CardDetails a = new CardDetails("4242", CardBrand.VISA, 12, 2027);
        CardDetails b = new CardDetails("4242", CardBrand.VISA, 12, 2028);
        assertThat(a).isNotEqualTo(b);
    }
}
