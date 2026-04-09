package com.payflow.webhook.api.dto;

import java.util.List;

public record DeliveryListResponse(List<DeliveryResponse> data, int totalElements) {
}
