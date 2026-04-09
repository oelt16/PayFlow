package com.payflow.merchant.domain.exception;

public final class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
