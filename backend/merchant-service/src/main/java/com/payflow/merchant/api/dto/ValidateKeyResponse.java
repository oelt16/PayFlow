package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for validated API key.
 */
public record ValidateKeyResponse(
        @JsonProperty("merchantId")
        String merchantId,
        @JsonProperty("keyHash")
        String keyHash,
        @JsonProperty("isActive")
        boolean isActive
) {}