package com.payflow.payment.api.ratelimit;

import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payment.api.security.MerchantContext;
import com.payflow.payment.domain.MerchantId;

import io.github.bucket4j.Bucket;

/**
 * Servlet filter that enforces per-merchant rate limits using token buckets.
 * <p>
 * Runs at {@code HIGHEST_PRECEDENCE + 15} — after authentication ({@code +10})
 * so MerchantContext is populated, but before idempotency ({@code +20}) so
 * rate-limited requests do not consume idempotency storage.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final BucketRegistry bucketRegistry;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(BucketRegistry bucketRegistry, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.bucketRegistry = bucketRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip if rate limiting is disabled
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip non-API paths
        String uri = request.getRequestURI();
        if (!uri.startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if no authenticated merchant in context
        if (!MerchantContext.isSet()) {
            filterChain.doFilter(request, response);
            return;
        }

        MerchantId merchantId = MerchantContext.require();
        String method = request.getMethod();

        // Resolve endpoint-specific limit (stricter limits override default)
        EndpointRateLimit endpointLimit = bucketRegistry.resolveEndpointLimit(method, uri);

        // Get or create bucket for this merchant + endpoint
        Bucket bucket = bucketRegistry.getBucket(merchantId, endpointLimit);

        // Determine limit for headers
        long limit = endpointLimit != null ? endpointLimit.tokens() : properties.getBurstCapacity();

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Allowed — add rate limit headers and continue
            long remaining = bucket.getAvailableTokens();
            long resetEpochSeconds = computeResetEpochSeconds(bucket, endpointLimit);
            response.setHeader(HEADER_LIMIT, String.valueOf(limit));
            response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
            response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds));
            filterChain.doFilter(request, response);
        } else {
            // Rate limited — return 429
            long retryAfterSeconds = computeRetryAfterSeconds(bucket, endpointLimit);
            long resetEpochSeconds = computeResetEpochSeconds(bucket, endpointLimit);

            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HEADER_LIMIT, String.valueOf(limit));
            response.setHeader(HEADER_REMAINING, "0");
            response.setHeader(HEADER_RESET, String.valueOf(resetEpochSeconds));
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));

            String requestId = resolveRequestId(request);
            RateLimitResponse body = RateLimitResponse.of(requestId, retryAfterSeconds);
            objectMapper.writeValue(response.getOutputStream(), body);
        }
    }

    private long computeResetEpochSeconds(Bucket bucket, EndpointRateLimit endpointLimit) {
        Duration refillDuration = endpointLimit != null
                ? endpointLimit.refillDuration()
                : Duration.ofMinutes(1);
        return System.currentTimeMillis() / 1000 + refillDuration.getSeconds();
    }

    private long computeRetryAfterSeconds(Bucket bucket, EndpointRateLimit endpointLimit) {
        Duration refillDuration = endpointLimit != null
                ? endpointLimit.refillDuration()
                : Duration.ofMinutes(1);
        long refillSeconds = refillDuration.getSeconds();
        // Estimate: time until at least 1 token is available
        long tokensNeeded = 1;
        long estimatedSeconds = Math.max(1, (tokensNeeded * refillSeconds) / Math.max(1, endpointLimit != null ? endpointLimit.tokens() : properties.getBurstCapacity()));
        return Math.max(1, estimatedSeconds);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute("requestId");
        return attr != null ? attr.toString() : "unknown";
    }
}
