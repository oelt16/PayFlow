package com.payflow.payment.domain;

import com.payflow.payment.domain.exception.InvalidCurrencyException;
import com.payflow.payment.domain.exception.NegativeAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new NegativeAmountException("Amount must not be negative: " + amount);
        }
        String normalizedCurrency = currency.toUpperCase();
        if ("XXX".equals(normalizedCurrency)) {
            throw new InvalidCurrencyException("Invalid currency code: " + currency);
        }
        try {
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException("Invalid currency code: " + currency);
        }
        BigDecimal scaled = amount.setScale(SCALE, ROUNDING);
        return new Money(scaled, normalizedCurrency);
    }

    public static Money zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new NegativeAmountException("Subtraction would yield negative amount");
        }
        return new Money(result.setScale(SCALE, ROUNDING), currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
