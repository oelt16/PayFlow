package com.payflow.webhook.infrastructure.kafka;

import com.payflow.webhook.application.port.WebhookDlqPublisher;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(KafkaTemplate.class)
public class NoOpWebhookDlqPublisher implements WebhookDlqPublisher {

    @Override
    public void publishFailedDelivery(WebhookEndpoint endpoint, WebhookDelivery delivery) {
        // No Kafka configured (e.g. local tests without broker).
    }
}
