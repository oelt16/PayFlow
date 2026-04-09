package com.payflow.webhook.application.port;

public record WebhookSendResult(boolean success, Integer statusCode, String errorMessage) {

    public static WebhookSendResult ok(int statusCode) {
        return new WebhookSendResult(true, statusCode, null);
    }

    public static WebhookSendResult fail(String message) {
        return new WebhookSendResult(false, null, message);
    }

    public static WebhookSendResult fail(int statusCode, String message) {
        return new WebhookSendResult(false, statusCode, message);
    }
}
