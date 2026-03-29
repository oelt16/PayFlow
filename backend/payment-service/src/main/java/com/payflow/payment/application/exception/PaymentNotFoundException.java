package com.payflow.payment.application.exception;

public final class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super("No payment found with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public String paymentId() {
        return paymentId;
    }
}
