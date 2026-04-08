package com.payflow.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payflow.payment.infrastructure.persistence.jpa.OutboxEventJpaEntity;
import com.payflow.payment.infrastructure.persistence.jpa.OutboxEventSpringDataRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventSpringDataRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ObjectMapper objectMapper;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Clock clock = Clock.fixed(Instant.parse("2024-09-01T12:00:00Z"), ZoneOffset.UTC);

        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        relay = new OutboxRelay(
                outboxRepository,
                kafkaTemplate,
                objectMapper,
                clock,
                transactionTemplate,
                "payments.events",
                100,
                30L
        );
    }

    @Test
    void publishUnpublishedOutboxEvents_sendsEnvelopeWithMerchantIdAsKeyAndMarksPublished() throws Exception {
        UUID rowId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant createdAt = Instant.parse("2024-08-15T10:00:00Z");
        OutboxEventJpaEntity row = new OutboxEventJpaEntity();
        row.setId(rowId);
        row.setAggregateId("pay_abc");
        row.setEventType("payment.created");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchantId", "mer_xyz");
        payload.put("paymentId", "pay_abc");
        payload.put("amount", "100.00");
        row.setPayload(payload);
        row.setCreatedAt(createdAt);
        row.setPublished(false);

        when(outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(List.of(row));
        when(outboxRepository.findById(rowId)).thenReturn(Optional.of(row));

        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = org.mockito.Mockito.mock(SendResult.class);
        when(kafkaTemplate.send(eq("payments.events"), eq("mer_xyz"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        relay.publishUnpublishedOutboxEvents();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("payments.events"), eq("mer_xyz"), jsonCaptor.capture());

        JsonNode envelope = objectMapper.readTree(jsonCaptor.getValue());
        assertThat(envelope.get("eventType").asText()).isEqualTo("payment.created");
        assertThat(envelope.get("aggregateId").asText()).isEqualTo("pay_abc");
        assertThat(envelope.get("merchantId").asText()).isEqualTo("mer_xyz");
        assertThat(envelope.get("eventId").asText()).isEqualTo("evt_" + rowId.toString().replace("-", ""));
        assertThat(envelope.get("occurredAt").asText()).isEqualTo("2024-08-15T10:00:00Z");
        assertThat(envelope.get("payload").get("merchantId").asText()).isEqualTo("mer_xyz");

        assertThat(row.isPublished()).isTrue();
        assertThat(row.getPublishedAt()).isEqualTo(Instant.parse("2024-09-01T12:00:00Z"));
        verify(outboxRepository).save(row);
    }

    @Test
    void publishRow_whenKafkaFails_doesNotMarkPublished() throws Exception {
        UUID rowId = UUID.randomUUID();
        OutboxEventJpaEntity row = new OutboxEventJpaEntity();
        row.setId(rowId);
        row.setAggregateId("pay_abc");
        row.setEventType("payment.created");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("merchantId", "mer_xyz");
        row.setPayload(payload);
        row.setCreatedAt(Instant.now());
        row.setPublished(false);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new ExecutionException("down", new RuntimeException())));

        relay.publishRow(row);

        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void publishRow_whenMerchantIdMissing_doesNotSend() {
        UUID rowId = UUID.randomUUID();
        OutboxEventJpaEntity row = new OutboxEventJpaEntity();
        row.setId(rowId);
        row.setAggregateId("pay_abc");
        row.setEventType("payment.created");
        row.setPayload(Map.of("paymentId", "pay_abc"));
        row.setCreatedAt(Instant.now());
        row.setPublished(false);

        relay.publishRow(row);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }
}
