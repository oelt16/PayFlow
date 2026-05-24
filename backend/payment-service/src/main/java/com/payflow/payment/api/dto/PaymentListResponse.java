package com.payflow.payment.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated list of payments")
public record PaymentListResponse(
        @Schema(description = "Payment items on this page") List<PaymentResponse> content,
        @Schema(description = "Total number of payments matching the filter", example = "100") long totalElements,
        @Schema(description = "Current page number (zero-indexed)", example = "0") int page,
        @Schema(description = "Page size", example = "20") int size
) {
}
