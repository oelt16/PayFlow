package com.payflow.payment.application.money;

import com.payflow.payment.domain.Money;

import java.math.BigDecimal;

public final class MoneyMinorUnits {

    private MoneyMinorUnits() {
    }

    public static Money toMoney(long amountMinor, String currency) {
        BigDecimal major = BigDecimal.valueOf(amountMinor).movePointLeft(2);
        return Money.of(major, currency);
    }

    public static long toMinorUnits(Money money) {
        return money.amount().movePointRight(2).setScale(0, java.math.RoundingMode.UNNECESSARY).longValueExact();
    }
}
