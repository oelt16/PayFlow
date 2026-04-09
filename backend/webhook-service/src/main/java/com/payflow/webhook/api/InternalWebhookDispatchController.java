package com.payflow.webhook.api;

import com.payflow.webhook.api.dto.DispatchRequest;
import com.payflow.webhook.application.InternalDispatchProperties;
import com.payflow.webhook.application.WebhookApplicationService;
import com.payflow.webhook.domain.MerchantId;

import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/webhooks")
@Validated
public class InternalWebhookDispatchController {

    private final WebhookApplicationService webhookApplicationService;
    private final InternalDispatchProperties internalDispatchProperties;
    private final ObjectMapper objectMapper;

    public InternalWebhookDispatchController(
            WebhookApplicationService webhookApplicationService,
            InternalDispatchProperties internalDispatchProperties,
            ObjectMapper objectMapper
    ) {
        this.webhookApplicationService = webhookApplicationService;
        this.internalDispatchProperties = internalDispatchProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/dispatch")
    public ResponseEntity<Void> dispatch(@Valid @RequestBody DispatchRequest body) {
        if (!internalDispatchProperties.isDispatchEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(body.getEventPayload());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid eventPayload", e);
        }
        webhookApplicationService.dispatch(
                MerchantId.of(body.getMerchantId()),
                body.getEventType(),
                payloadJson
        );
        return ResponseEntity.noContent().build();
    }
}
