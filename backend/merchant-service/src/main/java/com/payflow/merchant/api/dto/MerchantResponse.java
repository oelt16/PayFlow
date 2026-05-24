package com.payflow.merchant.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Merchant profile details")
public record MerchantResponse(
        @Schema(description = "Unique merchant identifier", example = "m_abc123") String id,
        @Schema(description = "Merchant business name", example = "Acme Corp") String name,
        @Schema(description = "Merchant contact email", example = "admin@acme.com") String email,
        @Schema(description = "Whether the merchant account is active", example = "true") boolean active,
        @Schema(description = "Registration timestamp", example = "2024-01-01T00:00:00Z") Instant createdAt
) {
}
