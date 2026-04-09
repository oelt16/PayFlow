package com.payflow.notification.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.event.PaymentEventEnvelope;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WebhookDispatchClient {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WebhookDispatchProperties properties;

    public WebhookDispatchClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            WebhookDispatchProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public void notifyWebhookService(PaymentEventEnvelope envelope) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventPayload = objectMapper.convertValue(envelope, Map.class);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("merchantId", envelope.merchantId());
            body.put("eventType", envelope.eventType());
            body.put("eventPayload", eventPayload);
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<Void> response = restClient.post()
                    .uri("/internal/webhooks/dispatch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toEntity(Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn(
                        "Webhook dispatch returned status {} for eventType={}",
                        response.getStatusCode(),
                        envelope.eventType()
                );
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to build webhook dispatch body: {}", e.getMessage());
        } catch (RestClientException e) {
            log.warn(
                    "Webhook dispatch HTTP error for eventType={}: {}",
                    envelope.eventType(),
                    e.getMessage()
            );
        }
    }
}
