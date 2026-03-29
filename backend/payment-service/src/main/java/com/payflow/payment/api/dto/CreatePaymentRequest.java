package com.payflow.payment.api.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreatePaymentRequest {

    @NotNull
    @Positive
    private Long amount;

    @NotBlank
    private String currency;

    private String description;

    @NotNull
    @Valid
    private CardPayload card;

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
