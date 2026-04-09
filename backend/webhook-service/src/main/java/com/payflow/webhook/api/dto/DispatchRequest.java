package com.payflow.webhook.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DispatchRequest {

    @NotBlank
    private String merchantId;

    @NotBlank
    private String eventType;

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
