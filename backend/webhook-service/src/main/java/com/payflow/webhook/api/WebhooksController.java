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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Webhooks", description = "Webhook registration, deactivation, and delivery management")
public class WebhooksController {

    private final WebhookApplicationService webhookApplicationService;

    public WebhooksController(WebhookApplicationService webhookApplicationService) {
        this.webhookApplicationService = webhookApplicationService;
    }

    @PostMapping
    @Operation(summary = "Register a webhook endpoint", description = "Registers a new webhook endpoint to receive event notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook registered — returns signing secret", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public ResponseEntity<WebhookRegisteredResponse> register(@Valid @RequestBody RegisterWebhookRequest body) {
        RegisteredWebhook created = webhookApplicationService.register(
                MerchantContext.require(),
                body.getUrl(),
                new LinkedHashSet<>(body.getEvents())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookApiMapper.toRegistered(created));
    }

    @GetMapping
    @Operation(summary = "List webhooks", description = "Lists all registered webhook endpoints for the authenticated merchant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of webhook endpoints", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public WebhookListResponse list() {
        List<WebhookSummaryResponse> content = webhookApplicationService.listEndpoints(MerchantContext.require()).stream()
                .map(WebhookApiMapper::toSummary)
                .collect(Collectors.toList());
        return new WebhookListResponse(content);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a webhook", description = "Deactivates a registered webhook endpoint by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Webhook deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Webhook not found", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        webhookApplicationService.deactivate(MerchantContext.require(), WebhookId.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    @Operation(summary = "List deliveries for a webhook", description = "Lists delivery history for a specific webhook endpoint")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of delivery attempts", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public DeliveryListResponse deliveries(@PathVariable String id) {
        List<DeliveryResponse> data = webhookApplicationService
                .listDeliveries(MerchantContext.require(), WebhookId.of(id)).stream()
                .map(WebhookApiMapper::toDelivery)
                .collect(Collectors.toList());
        return new DeliveryListResponse(data, data.size());
    }
}
