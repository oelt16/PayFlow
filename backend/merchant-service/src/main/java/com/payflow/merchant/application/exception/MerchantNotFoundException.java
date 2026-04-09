package com.payflow.merchant.application.exception;

public final class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String message) {
        super(message);
    }
}
