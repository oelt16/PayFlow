package com.payflow.webhook.infrastructure.persistence.jpa;

import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookDeliveryId;
import com.payflow.webhook.domain.WebhookEndpoint;
import com.payflow.webhook.domain.WebhookId;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class WebhookPersistenceMapper {

    public WebhookEndpoint toEndpointDomain(WebhookEndpointJpaEntity e) {
        Set<String> types = new LinkedHashSet<>(e.getEventTypes());
        return WebhookEndpoint.restore(
                WebhookId.of(e.getId()),
                MerchantId.of(e.getMerchantId()),
                e.getUrl(),
                e.getSecret(),
                types,
                e.isActive(),
                e.getCreatedAt()
        );
    }

    public void mergeEndpoint(WebhookEndpoint domain, WebhookEndpointJpaEntity e) {
        e.setUrl(domain.url());
        e.setSecret(domain.secret());
        e.setEventTypes(domain.eventTypes().stream().toList());
        e.setActive(domain.active());
    }

    public WebhookEndpointJpaEntity toNewEndpointEntity(WebhookEndpoint domain) {
        WebhookEndpointJpaEntity e = new WebhookEndpointJpaEntity();
        e.setId(domain.id().value());
        e.setMerchantId(domain.merchantId().value());
        e.setUrl(domain.url());
        e.setSecret(domain.secret());
        e.setEventTypes(domain.eventTypes().stream().toList());
        e.setActive(domain.active());
        e.setCreatedAt(domain.createdAt());
        return e;
    }

    public WebhookDelivery toDeliveryDomain(WebhookDeliveryJpaEntity e) {
        return WebhookDelivery.restore(
                WebhookDeliveryId.of(e.getId()),
                WebhookId.of(e.getWebhookId()),
                e.getEventType(),
                e.getEventPayloadJson(),
                e.getStatus(),
                e.getAttempts(),
                e.getLastAttemptAt(),
                e.getNextRetryAt(),
                e.getLastError(),
                e.getCreatedAt()
        );
    }

    public WebhookDeliveryJpaEntity toNewDeliveryEntity(WebhookDelivery domain) {
        WebhookDeliveryJpaEntity e = new WebhookDeliveryJpaEntity();
        e.setId(domain.id().value());
        e.setWebhookId(domain.webhookId().value());
        e.setEventType(domain.eventType());
        e.setEventPayloadJson(domain.eventPayloadJson());
        e.setStatus(domain.status());
        e.setAttempts(domain.attempts());
        e.setLastAttemptAt(domain.lastAttemptAt());
        e.setNextRetryAt(domain.nextRetryAt());
        e.setLastError(domain.lastError());
        e.setCreatedAt(domain.createdAt());
        return e;
    }

    public void mergeDelivery(WebhookDelivery domain, WebhookDeliveryJpaEntity e) {
        e.setStatus(domain.status());
        e.setAttempts(domain.attempts());
        e.setLastAttemptAt(domain.lastAttemptAt());
        e.setNextRetryAt(domain.nextRetryAt());
        e.setLastError(domain.lastError());
    }
}
