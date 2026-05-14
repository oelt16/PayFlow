package com.payflow.payment.application;

import java.util.Map;

/**
 * Result of an idempotency key lookup.
 * Contains the cached response body and HTTP status code.
 */
public record IdempotencyResult(
        Map<String, Object> responseBody,
        int httpStatus
) {
}