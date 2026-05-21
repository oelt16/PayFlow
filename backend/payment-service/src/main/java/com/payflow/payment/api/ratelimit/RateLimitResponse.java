package com.payflow.payment.api.ratelimit;

/**
 * 429 Too Many Requests response body.
 */
public record RateLimitResponse(
        ErrorDetail error
) {
    public record ErrorDetail(
            String code,
            String message,
            String requestId
    ) {}

    public static RateLimitResponse of(String requestId, long retryAfterSeconds) {
        return new RateLimitResponse(
                new ErrorDetail(
                        "rate_limit_exceeded",
                        "Too many requests. Retry after %d seconds.".formatted(retryAfterSeconds),
                        requestId
                )
        );
    }
}
