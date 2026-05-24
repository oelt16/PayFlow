package com.payflow.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CancelPaymentRequest {

    @Schema(description = "Reason for cancellation", example = "Customer changed their mind")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
