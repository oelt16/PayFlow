package com.payflow.webhook.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.payflow.webhook.domain.cache.ValidatedMerchant;

import java.time.Duration;
import java.util.Optional;

/**
 * Caffeine-based implementation of {@link ApiKeyCache}.
 */
public class CaffeineApiKeyCache implements ApiKeyCache {

    private final Cache<String, ValidatedMerchant> cache;

    /**
     * Creates a new CaffeineApiKeyCache.
     * @param ttlSeconds time-to-live for cache entries in seconds
     * @param maxSize maximum number of entries in the cache
     */
    public CaffeineApiKeyCache(int ttlSeconds, int maxSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public Optional<ValidatedMerchant> get(String keyPrefix) {
        return Optional.ofNullable(cache.getIfPresent(keyPrefix));
    }

    @Override
    public void put(String keyPrefix, ValidatedMerchant merchant) {
        cache.put(keyPrefix, merchant);
    }

    @Override
    public void evict(String keyPrefix) {
        cache.invalidate(keyPrefix);
    }
}