package com.payflow.payment.application;

import java.util.Map;

public record CreatePaymentCommand(
        long amountMinor,
        String currency,
        String description,
        String cardNumber,
        int expMonth,
        int expYear,
        String cvc,
        Map<String, String> metadata
) {
}
