package com.payflow.payment.domain;

import java.util.Objects;

public final class CardDetails {

    private final String last4;
    private final CardBrand brand;
    private final int expiryMonth;
    private final int expiryYear;

    public CardDetails(String last4, CardBrand brand, int expiryMonth, int expiryYear) {
        this.last4 = Objects.requireNonNull(last4, "last4");
        this.brand = Objects.requireNonNull(brand, "brand");
        if (last4.length() != 4 || !last4.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("last4 must be four digits");
        }
        if (expiryMonth < 1 || expiryMonth > 12) {
            throw new IllegalArgumentException("expiryMonth must be 1-12");
        }
        if (expiryYear < 2000 || expiryYear > 9999) {
            throw new IllegalArgumentException("expiryYear out of range");
        }
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
    }

    public String last4() {
        return last4;
    }

    public CardBrand brand() {
        return brand;
    }

    public int expiryMonth() {
        return expiryMonth;
    }

    public int expiryYear() {
        return expiryYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CardDetails that = (CardDetails) o;
        return expiryMonth == that.expiryMonth
                && expiryYear == that.expiryYear
                && last4.equals(that.last4)
                && brand == that.brand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(last4, brand, expiryMonth, expiryYear);
    }
}
