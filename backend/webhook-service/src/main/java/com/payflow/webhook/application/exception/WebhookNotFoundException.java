package com.payflow.webhook.application.exception;

public final class WebhookNotFoundException extends RuntimeException {

    public WebhookNotFoundException(String message) {
        super(message);
    }
}
