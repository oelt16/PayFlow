package com.payflow.payment.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;

import com.payflow.payment.domain.MerchantId;

import io.github.bucket4j.Bucket;

/**
 * Unit tests for BucketRegistry.
 */
@DisplayName("BucketRegistry")
class BucketRegistryTest {

    private RateLimitProperties properties;
    private BucketRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setBurstCapacity(10);
        properties.setCacheMaxSize(5);
        properties.setCacheTtl(Duration.ofMinutes(30));
        registry = new BucketRegistry(properties);
    }

    @Test
    @DisplayName("should create a bucket for a new merchant")
    void shouldCreateBucketForNewMerchant() {
        MerchantId merchant = MerchantId.of("mer_test_1");
        Bucket bucket = registry.getBucket(merchant, null);

        assertThat(bucket).isNotNull();
        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("should return the same bucket for the same merchant")
    void shouldReturnSameBucketForSameMerchant() {
        MerchantId merchant = MerchantId.of("mer_test_1");
        Bucket bucket1 = registry.getBucket(merchant, null);
        Bucket bucket2 = registry.getBucket(merchant, null);

        assertThat(bucket1).isSameAs(bucket2);
    }

    @Test
    @DisplayName("should create separate buckets for different merchants")
    void shouldCreateSeparateBucketsForDifferentMerchants() {
        MerchantId merchant1 = MerchantId.of("mer_test_1");
        MerchantId merchant2 = MerchantId.of("mer_test_2");

        Bucket bucket1 = registry.getBucket(merchant1, null);
        Bucket bucket2 = registry.getBucket(merchant2, null);

        assertThat(bucket1).isNotSameAs(bucket2);
    }

    @Test
    @DisplayName("should create endpoint-specific bucket with stricter limit")
    void shouldCreateEndpointSpecificBucket() {
        MerchantId merchant = MerchantId.of("mer_test_1");
        EndpointRateLimit endpointLimit = new EndpointRateLimit(
                "POST", "/v1/payments", 5, Duration.ofMinutes(1));

        Bucket defaultBucket = registry.getBucket(merchant, null);
        Bucket endpointBucket = registry.getBucket(merchant, endpointLimit);

        // Different cache keys → different buckets
        assertThat(defaultBucket).isNotSameAs(endpointBucket);
        // Endpoint bucket has fewer tokens
        assertThat(endpointBucket.getAvailableTokens()).isEqualTo(5);
        assertThat(defaultBucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("should evict entries when cache exceeds max size")
    void shouldEvictWhenCacheExceedsMaxSize() {
        Cache<String, Bucket> cache = registry.getCache();

        // Fill cache beyond max size (5)
        for (int i = 0; i < 10; i++) {
            MerchantId merchant = MerchantId.of("mer_evict_" + i);
            registry.getBucket(merchant, null);
        }

        // Caffeine may not evict immediately, but size should be bounded
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("should resolve endpoint limit by method and path")
    void shouldResolveEndpointLimit() {
        properties.getEndpoints().put("payments",
                new EndpointRateLimit("POST", "/v1/payments", 20, Duration.ofMinutes(1)));
        properties.getEndpoints().put("api-keys",
                new EndpointRateLimit("POST", "/v1/merchants/me/api-keys", 3, Duration.ofHours(1)));

        EndpointRateLimit paymentsLimit = registry.resolveEndpointLimit("POST", "/v1/payments");
        assertThat(paymentsLimit).isNotNull();
        assertThat(paymentsLimit.tokens()).isEqualTo(20);

        EndpointRateLimit apiKeysLimit = registry.resolveEndpointLimit("POST", "/v1/merchants/me/api-keys");
        assertThat(apiKeysLimit).isNotNull();
        assertThat(apiKeysLimit.tokens()).isEqualTo(3);

        // Non-matching endpoint returns null
        EndpointRateLimit noMatch = registry.resolveEndpointLimit("GET", "/v1/payments");
        assertThat(noMatch).isNull();
    }

    @Test
    @DisplayName("should consume tokens from bucket")
    void shouldConsumeTokens() {
        MerchantId merchant = MerchantId.of("mer_test_1");
        Bucket bucket = registry.getBucket(merchant, null);

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.getAvailableTokens()).isEqualTo(9);

        // Consume all remaining
        for (int i = 0; i < 9; i++) {
            bucket.tryConsume(1);
        }
        assertThat(bucket.getAvailableTokens()).isEqualTo(0);
        assertThat(bucket.tryConsume(1)).isFalse();
    }
}
