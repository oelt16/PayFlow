package com.payflow.webhook.domain;

public final class InvalidWebhookUrlException extends DomainException {

    public InvalidWebhookUrlException(String message) {
        super(message);
    }
}
