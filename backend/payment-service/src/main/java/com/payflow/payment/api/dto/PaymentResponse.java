package com.payflow.payment.api.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment details")
public record PaymentResponse(
        @Schema(description = "Unique payment identifier", example = "pay_abc123") String id,
        @Schema(description = "Payment amount in smallest currency unit", example = "2500") long amount,
        @Schema(description = "Three-letter ISO currency code", example = "USD") String currency,
        @Schema(description = "Payment status", example = "succeeded", allowableValues = {"pending", "succeeded", "failed", "captured", "cancelled"}) String status,
        @Schema(description = "Payment description", example = "Order #1234") String description,
        @Schema(description = "Client secret for 3DS authentication", example = "secret_abc123") String clientSecret,
        @Schema(description = "Optional key-value metadata") Map<String, String> metadata,
        @Schema(description = "Card used for payment") CardResponse card,
        @Schema(description = "Creation timestamp", example = "2024-01-01T00:00:00Z") String createdAt,
        @Schema(description = "Expiration timestamp", example = "2024-01-31T00:00:00Z") String expiresAt,
        @Schema(description = "Capture timestamp, if captured", example = "2024-01-01T00:01:00Z") String capturedAt,
        @Schema(description = "Cancellation timestamp, if cancelled", example = "2024-01-01T00:02:00Z") String cancelledAt,
        @Schema(description = "Total amount refunded so far", example = "500") long totalRefunded,
        @Schema(description = "Amount still refundable", example = "2000") long amountRefunded
) {
}
