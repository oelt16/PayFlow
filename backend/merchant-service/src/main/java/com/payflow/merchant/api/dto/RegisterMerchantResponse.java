package com.payflow.merchant.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Merchant registration response — includes the raw API key (shown only once)")
public record RegisterMerchantResponse(
        @Schema(description = "Unique merchant identifier", example = "m_abc123") String id,
        @Schema(description = "Merchant business name", example = "Acme Corp") String name,
        @Schema(description = "Merchant contact email", example = "admin@acme.com") String email,
        @Schema(description = "Raw API key — shown only on registration", example = "pf_live_abc123def456") String apiKey,
        @Schema(description = "Registration timestamp", example = "2024-01-01T00:00:00Z") Instant createdAt
) {
}
