package com.payflow.webhook.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DispatchRequest {

    @Schema(description = "Merchant ID to dispatch the webhook for", example = "m_abc123")
    @NotBlank
    private String merchantId;

    @Schema(description = "Event type to dispatch", example = "payment.succeeded")
    @NotBlank
    private String eventType;

    @Schema(description = "Event payload as arbitrary JSON")
    @NotNull
    private JsonNode eventPayload;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public JsonNode getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(JsonNode eventPayload) {
        this.eventPayload = eventPayload;
    }
}
