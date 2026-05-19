package com.payflow.payment.cache;

import com.payflow.payment.domain.cache.ValidatedMerchant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyCacheTest {

    private ApiKeyCache cache;

    @BeforeEach
    void setUp() {
        cache = new CaffeineApiKeyCache(60, 100);
    }

    @Test
    void getReturnsEmptyWhenKeyNotPresent() {
        Optional<ValidatedMerchant> result = cache.get("pk_live_nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void putThenGetReturnsMerchant() {
        ValidatedMerchant merchant = new ValidatedMerchant(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "$2a$10$hashed",
                true,
                Instant.now()
        );
        cache.put("pk_live_abc123", merchant);

        Optional<ValidatedMerchant> result = cache.get("pk_live_abc123");
        assertThat(result).isPresent();
        assertThat(result.get().merchantId()).isEqualTo(merchant.merchantId());
        assertThat(result.get().keyHash()).isEqualTo(merchant.keyHash());
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    void evictRemovesCachedEntry() {
        ValidatedMerchant merchant = new ValidatedMerchant(
                UUID.randomUUID(),
                "$2a$10$hashed",
                true,
                Instant.now()
        );
        cache.put("pk_live_test", merchant);
        cache.evict("pk_live_test");

        Optional<ValidatedMerchant> result = cache.get("pk_live_test");
        assertThat(result).isEmpty();
    }

    @Test
    void getReturnsEmptyAfterTTLExpires() throws InterruptedException {
        // Create cache with 1 second TTL
        ApiKeyCache shortTtlCache = new CaffeineApiKeyCache(1, 100);
        ValidatedMerchant merchant = new ValidatedMerchant(
                UUID.randomUUID(),
                "$2a$10$hashed",
                true,
                Instant.now()
        );
        shortTtlCache.put("pk_live_expire", merchant);

        // Wait for TTL to expire
        Thread.sleep(1500);

        Optional<ValidatedMerchant> result = shortTtlCache.get("pk_live_expire");
        assertThat(result).isEmpty();
    }

    @Test
    void putOverwritesExistingEntry() {
        ValidatedMerchant merchant1 = new ValidatedMerchant(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "$2a$10$hash1",
                true,
                Instant.now()
        );
        ValidatedMerchant merchant2 = new ValidatedMerchant(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "$2a$10$hash2",
                false,
                Instant.now()
        );

        cache.put("pk_live_same", merchant1);
        cache.put("pk_live_same", merchant2);

        Optional<ValidatedMerchant> result = cache.get("pk_live_same");
        assertThat(result).isPresent();
        assertThat(result.get().merchantId()).isEqualTo(merchant2.merchantId());
        assertThat(result.get().isActive()).isFalse();
    }
}