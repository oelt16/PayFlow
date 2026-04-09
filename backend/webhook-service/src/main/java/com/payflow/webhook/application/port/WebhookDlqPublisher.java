package com.payflow.webhook.application.port;

import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;

public interface WebhookDlqPublisher {

    void publishFailedDelivery(WebhookEndpoint endpoint, WebhookDelivery delivery);
}
