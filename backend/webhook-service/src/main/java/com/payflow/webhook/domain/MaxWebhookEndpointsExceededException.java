package com.payflow.webhook.domain;

public final class MaxWebhookEndpointsExceededException extends DomainException {

    public MaxWebhookEndpointsExceededException(String message) {
        super(message);
    }
}
