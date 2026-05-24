package com.payflow.payment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateRefundRequest {

    @Schema(description = "Refund amount in smallest currency unit (e.g., cents)", example = "1000", minimum = "1")
    @NotNull
    @Min(1)
    private Long amount;

    @Schema(description = "Three-letter ISO currency code", example = "USD")
    @NotBlank
    private String currency;

    @Schema(description = "Reason for the refund", example = "Customer requested refund")
    private String reason;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
