package com.payflow.merchant.api.ratelimit;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;

import com.payflow.merchant.domain.MerchantId;

/**
 * Registry that maps each merchant to a token bucket stored in a Caffeine cache.
 * Buckets are created on-demand with the configured rate limit parameters.
 * Endpoint-specific stricter limits override the default configuration.
 */
public class BucketRegistry {

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> cache;

    public BucketRegistry(RateLimitProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getCacheMaxSize())
                .expireAfterAccess(properties.getCacheTtl())
                .build();
    }

    /**
     * Get or create a bucket for the given merchant, optionally using an
     * endpoint-specific rate limit configuration.
     *
     * @param merchantId    the authenticated merchant
     * @param endpointLimit endpoint-specific limit, or null for default
     * @return the token bucket for this merchant + endpoint combination
     */
    public Bucket getBucket(MerchantId merchantId, EndpointRateLimit endpointLimit) {
        String cacheKey = buildCacheKey(merchantId, endpointLimit);
        return cache.get(cacheKey, key -> createBucket(endpointLimit));
    }

    /**
     * Resolve the endpoint-specific rate limit for the given HTTP method and path.
     * Returns null if no stricter limit applies (use default).
     */
    public EndpointRateLimit resolveEndpointLimit(String method, String path) {
        for (EndpointRateLimit limit : properties.getEndpoints().values()) {
            if (limit.method().equalsIgnoreCase(method) && limit.path().equals(path)) {
                return limit;
            }
        }
        return null;
    }

    private String buildCacheKey(MerchantId merchantId, EndpointRateLimit endpointLimit) {
        if (endpointLimit != null) {
            return merchantId.value() + ":" + endpointLimit.method() + ":" + endpointLimit.path();
        }
        return merchantId.value() + ":default";
    }

    private Bucket createBucket(EndpointRateLimit endpointLimit) {
        long tokens;
        Duration refillDuration;

        if (endpointLimit != null) {
            tokens = endpointLimit.tokens();
            refillDuration = endpointLimit.refillDuration();
        } else {
            tokens = properties.getBurstCapacity();
            refillDuration = Duration.ofMinutes(1);
        }

        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(tokens)
                        .refillGreedy(tokens, refillDuration))
                .build();
    }

    // Visible for testing
    Cache<String, Bucket> getCache() {
        return cache;
    }
}
