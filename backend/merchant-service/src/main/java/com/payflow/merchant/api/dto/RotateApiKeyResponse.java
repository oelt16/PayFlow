package com.payflow.merchant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "New API key response")
public record RotateApiKeyResponse(
        @Schema(description = "The new raw API key", example = "pf_live_xyz789abc012") String apiKey
) {
}
