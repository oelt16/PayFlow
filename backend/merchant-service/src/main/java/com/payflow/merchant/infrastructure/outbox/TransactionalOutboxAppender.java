package com.payflow.merchant.infrastructure.outbox;

import com.payflow.merchant.application.port.DomainEventOutbox;
import com.payflow.merchant.domain.DomainEvent;
import com.payflow.merchant.infrastructure.persistence.jpa.OutboxEventJpaEntity;
import com.payflow.merchant.infrastructure.persistence.jpa.OutboxEventSpringDataRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class TransactionalOutboxAppender implements DomainEventOutbox {

    private final OutboxEventSpringDataRepository outboxRepository;
    private final MerchantEventPayloadMapper payloadMapper;
    private final Clock clock;

    public TransactionalOutboxAppender(
            OutboxEventSpringDataRepository outboxRepository,
            MerchantEventPayloadMapper payloadMapper,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.payloadMapper = payloadMapper;
        this.clock = clock;
    }

    @Override
    public void append(String aggregateId, List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        for (DomainEvent event : events) {
            OutboxEventJpaEntity row = new OutboxEventJpaEntity();
            row.setId(UUID.randomUUID());
            row.setAggregateId(aggregateId);
            row.setEventType(payloadMapper.eventType(event));
            row.setPayload(payloadMapper.payload(event));
            row.setCreatedAt(now);
            row.setPublished(false);
            outboxRepository.save(row);
        }
    }
}
