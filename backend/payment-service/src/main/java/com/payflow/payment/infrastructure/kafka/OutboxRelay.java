package com.payflow.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payment.infrastructure.persistence.jpa.OutboxEventJpaEntity;
import com.payflow.payment.infrastructure.persistence.jpa.OutboxEventSpringDataRepository;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventSpringDataRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final String topic;
    private final int batchSize;
    private final long sendTimeoutSeconds;

    public OutboxRelay(
            OutboxEventSpringDataRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            TransactionTemplate transactionTemplate,
            @Value("${payflow.outbox.topic:payments.events}") String topic,
            @Value("${payflow.outbox.batch-size:100}") int batchSize,
            @Value("${payflow.outbox.send-timeout-seconds:30}") long sendTimeoutSeconds
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${payflow.outbox.poll-interval-ms:500}")
    public void publishUnpublishedOutboxEvents() {
        List<OutboxEventJpaEntity> batch = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, batchSize)
        );
        for (OutboxEventJpaEntity row : batch) {
            publishRow(row);
        }
    }

    void publishRow(OutboxEventJpaEntity row) {
        Object merchantRaw = row.getPayload() != null ? row.getPayload().get("merchantId") : null;
        if (merchantRaw == null) {
            log.error("Outbox row {} missing merchantId in payload; skipping publish", row.getId());
            return;
        }
        String merchantId = String.valueOf(merchantRaw);
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                eventIdFromOutboxId(row.getId()),
                row.getEventType(),
                row.getAggregateId(),
                merchantId,
                row.getCreatedAt(),
                copyPayload(row.getPayload())
        );
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox row {} to JSON", row.getId(), e);
            return;
        }
        try {
            kafkaTemplate.send(topic, merchantId, json).get(sendTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing outbox row {}", row.getId());
            return;
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka publish failed for outbox row {}: {}", row.getId(), e.getMessage());
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEventJpaEntity managed = outboxRepository.findById(row.getId()).orElseThrow();
            managed.setPublished(true);
            managed.setPublishedAt(clock.instant());
            outboxRepository.save(managed);
        });
    }

    private static String eventIdFromOutboxId(java.util.UUID id) {
        return "evt_" + id.toString().replace("-", "");
    }

    private static Map<String, Object> copyPayload(Map<String, Object> payload) {
        if (payload == null) {
            return Map.of();
        }
        return new LinkedHashMap<>(payload);
    }
}
