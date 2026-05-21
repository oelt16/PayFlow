package com.payflow.merchant.api.ratelimit;

import java.time.Duration;

/**
 * Endpoint-specific rate limit configuration.
 *
 * @param method HTTP method (e.g. "POST")
 * @param path exact URI path (e.g. "/v1/merchants/me/api-keys")
 * @param tokens number of tokens allowed per period
 * @param refillDuration duration after which tokens fully refill
 */
public record EndpointRateLimit(
        String method,
        String path,
        long tokens,
        Duration refillDuration
) {
}
