package com.payflow.webhook.infrastructure.persistence.jpa;

import com.payflow.webhook.application.port.WebhookDeliveryRepository;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookId;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class JpaWebhookDeliveryRepositoryAdapter implements WebhookDeliveryRepository {

    private final WebhookDeliverySpringDataRepository springData;
    private final WebhookPersistenceMapper mapper;

    public JpaWebhookDeliveryRepositoryAdapter(
            WebhookDeliverySpringDataRepository springData,
            WebhookPersistenceMapper mapper
    ) {
        this.springData = springData;
        this.mapper = mapper;
    }

    @Override
    public void save(WebhookDelivery delivery) {
        if (springData.existsById(delivery.id().value())) {
            WebhookDeliveryJpaEntity e = springData.findById(delivery.id().value()).orElseThrow();
            mapper.mergeDelivery(delivery, e);
            springData.save(e);
        } else {
            springData.save(mapper.toNewDeliveryEntity(delivery));
        }
    }

    @Override
    public List<WebhookDelivery> findByWebhookId(WebhookId webhookId) {
        return springData.findByWebhookIdOrderByCreatedAtAsc(webhookId.value()).stream()
                .map(mapper::toDeliveryDomain)
                .toList();
    }

    @Override
    public List<WebhookDelivery> findPendingDue(Instant now, int limit) {
        return springData.findPendingDue(now, PageRequest.of(0, limit)).stream()
                .map(mapper::toDeliveryDomain)
                .toList();
    }
}
