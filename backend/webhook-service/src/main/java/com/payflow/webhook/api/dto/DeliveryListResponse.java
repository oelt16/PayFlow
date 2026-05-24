package com.payflow.webhook.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated list of delivery attempts")
public record DeliveryListResponse(
        @Schema(description = "Delivery attempt items") List<DeliveryResponse> data,
        @Schema(description = "Total number of delivery attempts", example = "10") int totalElements
) {
}
