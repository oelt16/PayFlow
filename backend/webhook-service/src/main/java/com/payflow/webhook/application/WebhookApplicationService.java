package com.payflow.webhook.application;

import com.payflow.webhook.application.exception.WebhookNotFoundException;
import com.payflow.webhook.application.port.WebhookDeliveryRepository;
import com.payflow.webhook.application.port.WebhookDlqPublisher;
import com.payflow.webhook.application.port.WebhookEndpointRepository;
import com.payflow.webhook.application.port.WebhookSendResult;
import com.payflow.webhook.application.port.WebhookSender;
import com.payflow.webhook.domain.DeliveryStatus;
import com.payflow.webhook.domain.MaxWebhookEndpointsExceededException;
import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;
import com.payflow.webhook.domain.WebhookId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookApplicationService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSender webhookSender;
    private final WebhookDlqPublisher dlqPublisher;
    private final WebhookProperties webhookProperties;
    private final Clock clock;

    public WebhookApplicationService(
            WebhookEndpointRepository endpointRepository,
            WebhookDeliveryRepository deliveryRepository,
            WebhookSender webhookSender,
            WebhookDlqPublisher dlqPublisher,
            WebhookProperties webhookProperties,
            Clock clock
    ) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookSender = webhookSender;
        this.dlqPublisher = dlqPublisher;
        this.webhookProperties = webhookProperties;
        this.clock = clock;
    }

    @Transactional
    public RegisteredWebhook register(MerchantId merchantId, String url, Set<String> eventTypes) {
        if (endpointRepository.countActiveByMerchantId(merchantId) >= webhookProperties.getMaxEndpointsPerMerchant()) {
            throw new MaxWebhookEndpointsExceededException(
                    "Maximum " + webhookProperties.getMaxEndpointsPerMerchant() + " webhook endpoints per merchant"
            );
        }
        WebhookEndpoint endpoint = WebhookEndpoint.register(merchantId, url, eventTypes, clock.instant());
        endpointRepository.save(endpoint);
        return new RegisteredWebhook(
                endpoint.id(),
                endpoint.url(),
                endpoint.eventTypes(),
                endpoint.secret(),
                endpoint.createdAt()
        );
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listEndpoints(MerchantId merchantId) {
        return endpointRepository.findByMerchantId(merchantId);
    }

    @Transactional
    public void deactivate(MerchantId merchantId, WebhookId webhookId) {
        WebhookEndpoint endpoint = endpointRepository
                .findByIdAndMerchantId(webhookId, merchantId)
                .orElseThrow(() -> new WebhookNotFoundException("Webhook not found: " + webhookId.value()));
        endpoint.deactivate();
        endpointRepository.save(endpoint);
    }

    @Transactional(readOnly = true)
    public List<WebhookDelivery> listDeliveries(MerchantId merchantId, WebhookId webhookId) {
        endpointRepository
                .findByIdAndMerchantId(webhookId, merchantId)
                .orElseThrow(() -> new WebhookNotFoundException("Webhook not found: " + webhookId.value()));
        return deliveryRepository.findByWebhookId(webhookId);
    }

    @Transactional
    public void dispatch(MerchantId merchantId, String eventType, String eventPayloadJson) {
        List<WebhookEndpoint> targets = endpointRepository.findActiveByMerchantIdAndEventType(merchantId, eventType);
        Instant now = clock.instant();
        for (WebhookEndpoint endpoint : targets) {
            WebhookDelivery delivery = WebhookDelivery.createPending(endpoint.id(), eventType, eventPayloadJson, now);
            deliveryRepository.save(delivery);
            deliverOneAttempt(endpoint, delivery, now);
        }
    }

    @Transactional
    public void processDueDeliveries() {
        Instant now = clock.instant();
        List<WebhookDelivery> due = deliveryRepository.findPendingDue(now, 100);
        for (WebhookDelivery delivery : due) {
            WebhookEndpoint endpoint = endpointRepository
                    .findById(delivery.webhookId())
                    .orElse(null);
            if (endpoint == null) {
                continue;
            }
            deliverOneAttempt(endpoint, delivery, now);
        }
    }

    private void deliverOneAttempt(WebhookEndpoint endpoint, WebhookDelivery delivery, Instant now) {
        if (delivery.status() != DeliveryStatus.PENDING || !delivery.isDue(now)) {
            return;
        }
        WebhookSendResult result = webhookSender.send(
                endpoint.url(),
                delivery.eventPayloadJson(),
                endpoint.secret(),
                webhookProperties.getSignatureHeader()
        );
        if (result.success()
                && result.statusCode() != null
                && result.statusCode() >= 200
                && result.statusCode() < 300) {
            delivery.recordSuccess(now);
        } else {
            String err = result.errorMessage() != null
                    ? result.errorMessage()
                    : ("HTTP " + result.statusCode());
            delivery.recordFailure(err, now);
            if (delivery.status() == DeliveryStatus.FAILED) {
                dlqPublisher.publishFailedDelivery(endpoint, delivery);
            }
        }
        deliveryRepository.save(delivery);
    }
}
