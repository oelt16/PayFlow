package com.payflow.payment.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "List of refunds for a payment")
public record RefundListResponse(
        @Schema(description = "Refund items") List<RefundResponse> data,
        @Schema(description = "Total number of refunds", example = "5") int totalElements
) {
}
