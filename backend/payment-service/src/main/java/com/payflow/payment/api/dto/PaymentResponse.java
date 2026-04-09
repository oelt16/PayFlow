package com.payflow.payment.api.dto;

import java.util.Map;

public record PaymentResponse(
        String id,
        long amount,
        String currency,
        String status,
        String description,
        String clientSecret,
        Map<String, String> metadata,
        CardResponse card,
        String createdAt,
        String expiresAt,
        String capturedAt,
        String cancelledAt,
        long totalRefunded,
        long amountRefunded
) {
}
