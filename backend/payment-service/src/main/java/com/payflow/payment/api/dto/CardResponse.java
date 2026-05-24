package com.payflow.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Card summary (masked)")
public record CardResponse(
        @Schema(description = "Last 4 digits of the card", example = "4242") String last4,
        @Schema(description = "Card brand", example = "visa") String brand,
        @Schema(description = "Expiration month", example = "12") int expMonth,
        @Schema(description = "Expiration year", example = "2028") int expYear
) {
}
