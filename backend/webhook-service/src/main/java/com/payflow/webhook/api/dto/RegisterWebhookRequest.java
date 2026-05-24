package com.payflow.webhook.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RegisterWebhookRequest {

    @Schema(description = "Webhook endpoint URL that will receive event payloads", example = "https://example.com/webhooks")
    @NotBlank
    private String url;

    @Schema(description = "List of event types to subscribe to", example = "[\"payment.succeeded\", \"payment.failed\"]")
    @NotEmpty
    private List<@NotBlank String> events;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
