package com.payflow.payment.application.card;

import com.payflow.payment.domain.CardBrand;
import com.payflow.payment.domain.CardDetails;

public final class StubCardDetailsFactory {

    private StubCardDetailsFactory() {
    }

    public static CardDetails from(String rawPan, int expiryMonth, int expiryYear) {
        String digits = rawPan.replaceAll("\\D", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("card number must contain at least four digits");
        }
        String last4 = digits.substring(digits.length() - 4);
        CardBrand brand = brandFromPan(digits);
        return new CardDetails(last4, brand, expiryMonth, expiryYear);
    }

    private static CardBrand brandFromPan(String digits) {
        if (digits.startsWith("4")) {
            return CardBrand.VISA;
        }
        if (digits.startsWith("34") || digits.startsWith("37")) {
            return CardBrand.AMEX;
        }
        if (digits.startsWith("5")) {
            return CardBrand.MASTERCARD;
        }
        return CardBrand.VISA;
    }
}
