package com.payflow.payment.domain;

import com.payflow.payment.domain.exception.InvalidCurrencyException;
import com.payflow.payment.domain.exception.NegativeAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void negativeAmountThrows() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1), "USD"))
                .isInstanceOf(NegativeAmountException.class);
    }

    @Test
    void invalidCurrencyThrows() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "XXX"))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void equalitySameAmountAndCurrency() {
        Money a = Money.of(new BigDecimal("10.00"), "USD");
        Money b = Money.of(new BigDecimal("10.0"), "USD");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void addProducesCorrectSum() {
        Money a = Money.of(new BigDecimal("10.50"), "EUR");
        Money b = Money.of(new BigDecimal("2.25"), "EUR");
        assertThat(a.add(b)).isEqualTo(Money.of(new BigDecimal("12.75"), "EUR"));
    }

    @Test
    void subtractProducesCorrectDifference() {
        Money a = Money.of(new BigDecimal("10.00"), "GBP");
        Money b = Money.of(new BigDecimal("3.25"), "GBP");
        assertThat(a.subtract(b)).isEqualTo(Money.of(new BigDecimal("6.75"), "GBP"));
    }

    @Test
    void invalidNonIsoCurrencyThrows() {
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "ZZZ"))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void subtractBelowZeroThrows() {
        Money a = Money.of(new BigDecimal("5.00"), "USD");
        Money b = Money.of(new BigDecimal("6.00"), "USD");
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(NegativeAmountException.class);
    }

    @Test
    void addRequiresSameCurrency() {
        Money usd = Money.of(BigDecimal.TEN, "USD");
        Money eur = Money.of(BigDecimal.TEN, "EUR");
        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void subtractRequiresSameCurrency() {
        Money usd = Money.of(BigDecimal.TEN, "USD");
        Money eur = Money.of(BigDecimal.ONE, "EUR");
        assertThatThrownBy(() -> usd.subtract(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void zeroIsZeroAmountInCurrency() {
        assertThat(Money.zero("usd")).isEqualTo(Money.of(BigDecimal.ZERO, "USD"));
    }

    @Test
    void isLessOrEqual() {
        Money a = Money.of(new BigDecimal("10.00"), "USD");
        Money b = Money.of(new BigDecimal("10.00"), "USD");
        Money c = Money.of(new BigDecimal("9.99"), "USD");
        assertThat(c.isLessOrEqual(a)).isTrue();
        assertThat(a.isLessOrEqual(b)).isTrue();
        assertThat(a.isLessOrEqual(c)).isFalse();
    }

    @Test
    void isGreaterThanFalseWhenEqualOrLess() {
        Money ten = Money.of(new BigDecimal("10.00"), "USD");
        assertThat(ten.isGreaterThan(ten)).isFalse();
        assertThat(ten.isGreaterThan(Money.of(new BigDecimal("11.00"), "USD"))).isFalse();
    }

    @Test
    void equalsNullIsFalse() {
        assertThat(Money.of(BigDecimal.ONE, "USD").equals(null)).isFalse();
    }

    @Test
    void equalsDifferentTypeIsFalse() {
        assertThat(Money.of(BigDecimal.ONE, "USD").equals("1 USD")).isFalse();
    }

    @Test
    void equalsDifferentAmountSameCurrencyIsFalse() {
        Money a = Money.of(BigDecimal.ONE, "USD");
        Money b = Money.of(new BigDecimal("2.00"), "USD");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsSameAmountDifferentCurrencyIsFalse() {
        Money a = Money.of(BigDecimal.ONE, "USD");
        Money b = Money.of(BigDecimal.ONE, "EUR");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCodeMatchesValueSemantics() {
        Money a = Money.of(new BigDecimal("1.00"), "USD");
        Money b = Money.of(new BigDecimal("1.0"), "USD");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
