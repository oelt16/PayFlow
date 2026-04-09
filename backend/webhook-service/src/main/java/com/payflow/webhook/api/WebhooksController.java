package com.payflow.webhook.api;

import com.payflow.webhook.api.dto.DeliveryListResponse;
import com.payflow.webhook.api.dto.DeliveryResponse;
import com.payflow.webhook.api.dto.RegisterWebhookRequest;
import com.payflow.webhook.api.dto.WebhookListResponse;
import com.payflow.webhook.api.dto.WebhookRegisteredResponse;
import com.payflow.webhook.api.dto.WebhookSummaryResponse;
import com.payflow.webhook.api.security.MerchantContext;
import com.payflow.webhook.application.RegisteredWebhook;
import com.payflow.webhook.application.WebhookApplicationService;
import com.payflow.webhook.domain.WebhookId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/webhooks")
@Validated
public class WebhooksController {

    private final WebhookApplicationService webhookApplicationService;

    public WebhooksController(WebhookApplicationService webhookApplicationService) {
        this.webhookApplicationService = webhookApplicationService;
    }

    @PostMapping
    public ResponseEntity<WebhookRegisteredResponse> register(@Valid @RequestBody RegisterWebhookRequest body) {
        RegisteredWebhook created = webhookApplicationService.register(
                MerchantContext.require(),
                body.getUrl(),
                new LinkedHashSet<>(body.getEvents())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookApiMapper.toRegistered(created));
    }

    @GetMapping
    public WebhookListResponse list() {
        List<WebhookSummaryResponse> content = webhookApplicationService.listEndpoints(MerchantContext.require()).stream()
                .map(WebhookApiMapper::toSummary)
                .collect(Collectors.toList());
        return new WebhookListResponse(content);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        webhookApplicationService.deactivate(MerchantContext.require(), WebhookId.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    public DeliveryListResponse deliveries(@PathVariable String id) {
        List<DeliveryResponse> data = webhookApplicationService
                .listDeliveries(MerchantContext.require(), WebhookId.of(id)).stream()
                .map(WebhookApiMapper::toDelivery)
                .collect(Collectors.toList());
        return new DeliveryListResponse(data, data.size());
    }
}
