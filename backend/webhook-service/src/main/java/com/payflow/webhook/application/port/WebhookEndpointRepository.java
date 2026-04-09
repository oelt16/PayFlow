package com.payflow.webhook.application.port;

import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.WebhookEndpoint;
import com.payflow.webhook.domain.WebhookId;

import java.util.List;
import java.util.Optional;

public interface WebhookEndpointRepository {

    void save(WebhookEndpoint endpoint);

    long countActiveByMerchantId(MerchantId merchantId);

    List<WebhookEndpoint> findByMerchantId(MerchantId merchantId);

    Optional<WebhookEndpoint> findByIdAndMerchantId(WebhookId id, MerchantId merchantId);

    Optional<WebhookEndpoint> findById(WebhookId id);

    List<WebhookEndpoint> findActiveByMerchantIdAndEventType(MerchantId merchantId, String eventType);
}
