package com.payflow.payment.domain;

public enum PaymentStatus {
    PENDING,
    CAPTURED,
    CANCELLED,
    REFUNDED,
    PARTIAL_REFUND,
    EXPIRED
}
