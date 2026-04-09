package com.payflow.webhook.api.dto;

import java.util.List;

public record WebhookListResponse(List<WebhookSummaryResponse> content) {
}
