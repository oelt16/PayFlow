package com.payflow.payment.api.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreatePaymentRequest {

    @Schema(description = "Payment amount in smallest currency unit (e.g., cents)", example = "2500", minimum = "1")
    @NotNull
    @Positive
    private Long amount;

    @Schema(description = "Three-letter ISO currency code", example = "USD")
    @NotBlank
    private String currency;

    @Schema(description = "Optional payment description", example = "Order #1234")
    private String description;

    @Schema(description = "Card payment details")
    @NotNull
    @Valid
    private CardPayload card;

    @Schema(description = "Optional key-value metadata", example = "{\"order_id\": \"1234\"}")
    private Map<String, String> metadata = new HashMap<>();

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CardPayload getCard() {
        return card;
    }

    public void setCard(CardPayload card) {
        this.card = card;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}
