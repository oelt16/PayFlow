package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for validating API key prefix.
 */
public record ValidateKeyRequest(
        @JsonProperty("keyPrefix")
        String keyPrefix
) {}