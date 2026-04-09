package com.payflow.webhook.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliverySpringDataRepository extends JpaRepository<WebhookDeliveryJpaEntity, String> {

    List<WebhookDeliveryJpaEntity> findByWebhookIdOrderByCreatedAtAsc(String webhookId);

    @Query(
            """
                    SELECT d FROM WebhookDeliveryJpaEntity d
                    WHERE d.status = com.payflow.webhook.domain.DeliveryStatus.PENDING
                    AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
                    ORDER BY d.createdAt ASC
                    """
    )
    List<WebhookDeliveryJpaEntity> findPendingDue(@Param("now") Instant now, Pageable pageable);
}
