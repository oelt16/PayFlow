package com.payflow.payment.cache;

import com.payflow.payment.domain.cache.ValidatedMerchant;

import java.util.Optional;

/**
 * Cache interface for API key validation results.
 */
public interface ApiKeyCache {

    /**
     * Retrieves a validated merchant from the cache.
     * @param keyPrefix the API key prefix
     * @return the validated merchant if present and not expired
     */
    Optional<ValidatedMerchant> get(String keyPrefix);

    /**
     * Stores a validated merchant in the cache.
     * @param keyPrefix the API key prefix
     * @param merchant the validated merchant data
     */
    void put(String keyPrefix, ValidatedMerchant merchant);

    /**
     * Evicts a cached entry by key prefix.
     * @param keyPrefix the API key prefix to evict
     */
    void evict(String keyPrefix);
}