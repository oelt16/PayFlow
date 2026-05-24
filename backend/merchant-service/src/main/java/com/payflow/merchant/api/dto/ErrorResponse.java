package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard error response DTO (internal API).
 */
public record ErrorResponse(
        @JsonProperty("error")
        @Schema(description = "Error message", example = "key_not_found")
        String error
) {}