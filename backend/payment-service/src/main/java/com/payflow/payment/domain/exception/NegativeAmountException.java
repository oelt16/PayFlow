package com.payflow.payment.domain.exception;

public final class NegativeAmountException extends DomainException {

    public NegativeAmountException(String message) {
        super(message);
    }
}
