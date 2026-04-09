package com.payflow.webhook.infrastructure.scheduling;

import com.payflow.webhook.application.WebhookApplicationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookDeliveryRetryJob {

    private final WebhookApplicationService webhookApplicationService;

    public WebhookDeliveryRetryJob(WebhookApplicationService webhookApplicationService) {
        this.webhookApplicationService = webhookApplicationService;
    }

    @Scheduled(fixedDelayString = "${payflow.webhook.retry-poll-ms:5000}")
    public void processDue() {
        webhookApplicationService.processDueDeliveries();
    }
}
