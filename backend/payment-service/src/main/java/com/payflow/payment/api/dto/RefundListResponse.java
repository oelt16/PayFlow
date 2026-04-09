package com.payflow.payment.api.dto;

import java.util.List;

public record RefundListResponse(List<RefundResponse> data, int totalElements) {
}
