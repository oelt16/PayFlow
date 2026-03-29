package com.payflow.payment.api.dto;

import java.util.List;

public record PaymentListResponse(
        List<PaymentResponse> content,
        long totalElements,
        int page,
        int size
) {
}
