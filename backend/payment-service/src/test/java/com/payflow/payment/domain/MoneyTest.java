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
}
