package com.payflow.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refund details")
public record RefundResponse(
        @Schema(description = "Unique refund identifier", example = "ref_abc123") String id,
        @Schema(description = "ID of the refunded payment", example = "pay_abc123") String paymentId,
        @Schema(description = "Refund amount in smallest currency unit", example = "1000") long amount,
        @Schema(description = "Three-letter ISO currency code", example = "USD") String currency,
        @Schema(description = "Reason for the refund", example = "Customer requested refund") String reason,
        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z") String createdAt
) {
}
