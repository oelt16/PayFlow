package com.payflow.merchant.domain.exception;

public final class MerchantAlreadyDeactivatedException extends DomainException {

    public MerchantAlreadyDeactivatedException(String message) {
        super(message);
    }
}
