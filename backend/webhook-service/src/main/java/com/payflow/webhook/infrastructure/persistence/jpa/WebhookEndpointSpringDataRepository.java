package com.payflow.webhook.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEndpointSpringDataRepository extends JpaRepository<WebhookEndpointJpaEntity, String> {

    long countByMerchantIdAndActiveTrue(String merchantId);

    List<WebhookEndpointJpaEntity> findByMerchantIdOrderByCreatedAtAsc(String merchantId);

    Optional<WebhookEndpointJpaEntity> findByIdAndMerchantId(String id, String merchantId);

    List<WebhookEndpointJpaEntity> findByMerchantIdAndActiveTrueOrderByCreatedAtAsc(String merchantId);
}
