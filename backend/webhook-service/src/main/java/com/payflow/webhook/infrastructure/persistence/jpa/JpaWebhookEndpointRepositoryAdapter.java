package com.payflow.webhook.infrastructure.persistence.jpa;

import com.payflow.webhook.application.port.WebhookEndpointRepository;
import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.WebhookEndpoint;
import com.payflow.webhook.domain.WebhookId;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class JpaWebhookEndpointRepositoryAdapter implements WebhookEndpointRepository {

    private final WebhookEndpointSpringDataRepository springData;
    private final WebhookPersistenceMapper mapper;

    public JpaWebhookEndpointRepositoryAdapter(
            WebhookEndpointSpringDataRepository springData,
            WebhookPersistenceMapper mapper
    ) {
        this.springData = springData;
        this.mapper = mapper;
    }

    @Override
    public void save(WebhookEndpoint endpoint) {
        Optional<WebhookEndpointJpaEntity> existing = springData.findById(endpoint.id().value());
        if (existing.isPresent()) {
            WebhookEndpointJpaEntity e = existing.get();
            mapper.mergeEndpoint(endpoint, e);
            springData.save(e);
        } else {
            springData.save(mapper.toNewEndpointEntity(endpoint));
        }
    }

    @Override
    public long countActiveByMerchantId(MerchantId merchantId) {
        return springData.countByMerchantIdAndActiveTrue(merchantId.value());
    }

    @Override
    public List<WebhookEndpoint> findByMerchantId(MerchantId merchantId) {
        return springData.findByMerchantIdOrderByCreatedAtAsc(merchantId.value()).stream()
                .map(mapper::toEndpointDomain)
                .toList();
    }

    @Override
    public Optional<WebhookEndpoint> findByIdAndMerchantId(WebhookId id, MerchantId merchantId) {
        return springData.findByIdAndMerchantId(id.value(), merchantId.value()).map(mapper::toEndpointDomain);
    }

    @Override
    public Optional<WebhookEndpoint> findById(WebhookId id) {
        return springData.findById(id.value()).map(mapper::toEndpointDomain);
    }

    @Override
    public List<WebhookEndpoint> findActiveByMerchantIdAndEventType(MerchantId merchantId, String eventType) {
        return springData.findByMerchantIdAndActiveTrueOrderByCreatedAtAsc(merchantId.value()).stream()
                .map(mapper::toEndpointDomain)
                .filter(e -> e.matchesEvent(eventType))
                .toList();
    }
}
