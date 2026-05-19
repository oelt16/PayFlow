package com.payflow.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error response DTO.
 */
public record ErrorResponse(
        @JsonProperty("error")
        String error
) {}