package com.payflow.webhook.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.webhook.application.KafkaTopicProperties;
import com.payflow.webhook.application.port.WebhookDlqPublisher;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaWebhookDlqPublisher implements WebhookDlqPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaWebhookDlqPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String dlqTopic;

    public KafkaWebhookDlqPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopicProperties topicProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.dlqTopic = topicProperties.getDlqTopic();
    }

    @Override
    public void publishFailedDelivery(WebhookEndpoint endpoint, WebhookDelivery delivery) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deliveryId", delivery.id().value());
        body.put("webhookId", delivery.webhookId().value());
        body.put("merchantId", endpoint.merchantId().value());
        body.put("url", endpoint.url());
        body.put("eventType", delivery.eventType());
        body.put("attempts", delivery.attempts());
        body.put("lastError", delivery.lastError());
        body.put("eventPayload", delivery.eventPayloadJson());
        try {
            String json = objectMapper.writeValueAsString(body);
            kafkaTemplate.send(dlqTopic, endpoint.merchantId().value(), json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize DLQ message for delivery {}", delivery.id().value(), e);
        }
    }
}
