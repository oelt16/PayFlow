package com.payflow.webhook.application.port;

public interface WebhookSender {

    WebhookSendResult send(String url, String bodyJson, String secret, String signatureHeaderName);
}
