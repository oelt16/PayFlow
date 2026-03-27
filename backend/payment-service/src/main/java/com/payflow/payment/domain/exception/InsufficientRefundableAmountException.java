package com.payflow.payment.domain.exception;

public final class InsufficientRefundableAmountException extends DomainException {

    public InsufficientRefundableAmountException(String message) {
        super(message);
    }
}
