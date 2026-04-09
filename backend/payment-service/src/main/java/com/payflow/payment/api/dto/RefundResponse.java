package com.payflow.payment.api.dto;

public record RefundResponse(
        String id,
        String paymentId,
        long amount,
        String currency,
        String reason,
        String createdAt
) {
}
