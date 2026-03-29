package com.payflow.payment.application;

import com.payflow.payment.domain.Payment;

public record CreatedPaymentResult(Payment payment, String clientSecret) {
}
