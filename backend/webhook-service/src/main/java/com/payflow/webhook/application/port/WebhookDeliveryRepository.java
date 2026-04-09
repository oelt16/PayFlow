package com.payflow.webhook.application.port;

import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookId;

import java.time.Instant;
import java.util.List;

public interface WebhookDeliveryRepository {

    void save(WebhookDelivery delivery);

    List<WebhookDelivery> findByWebhookId(WebhookId webhookId);

    List<WebhookDelivery> findPendingDue(Instant now, int limit);
}
