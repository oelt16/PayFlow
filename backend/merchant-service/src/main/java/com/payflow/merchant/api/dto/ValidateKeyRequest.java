package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for validating API key prefix.
 */
public record ValidateKeyRequest(
        @JsonProperty("keyPrefix")
        @Schema(description = "First 8 characters of the API key (key prefix)", example = "pf_live_")
        String keyPrefix
) {}