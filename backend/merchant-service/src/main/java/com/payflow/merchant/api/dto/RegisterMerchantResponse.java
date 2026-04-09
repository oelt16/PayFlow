package com.payflow.merchant.api.dto;

import java.time.Instant;

public record RegisterMerchantResponse(
        String id,
        String name,
        String email,
        String apiKey,
        Instant createdAt
) {
}
