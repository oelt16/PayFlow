package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for validated API key.
 */
public record ValidateKeyResponse(
        @JsonProperty("merchantId")
        @Schema(description = "Merchant ID associated with the key", example = "m_abc123")
        String merchantId,
        @JsonProperty("keyHash")
        @Schema(description = "Hashed value of the API key", example = "$2a$10$...")
        String keyHash,
        @JsonProperty("isActive")
        @Schema(description = "Whether the key is active", example = "true")
        boolean isActive
) {}