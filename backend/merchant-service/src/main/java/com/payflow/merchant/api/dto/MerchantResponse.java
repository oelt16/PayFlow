package com.payflow.merchant.api.dto;

import java.time.Instant;

public record MerchantResponse(
        String id,
        String name,
        String email,
        boolean active,
        Instant createdAt
) {
}
