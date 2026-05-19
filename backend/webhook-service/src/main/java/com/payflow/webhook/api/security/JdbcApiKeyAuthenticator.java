package com.payflow.webhook.api.security;

import com.payflow.webhook.cache.ApiKeyCache;
import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.cache.ValidatedMerchant;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Resolves {@link MerchantId} from a raw API key using cache + HTTP approach.
 * First checks local Caffeine cache, then calls merchant-service on cache miss.
 */
@Component
public class JdbcApiKeyAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(JdbcApiKeyAuthenticator.class);
    private static final int KEY_PREFIX_LENGTH = 8;

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final ApiKeyCache apiKeyCache;
    private final RestTemplate restTemplate;

    @Value("${payflow.api-key-cache.internal-endpoint.base-url}")
    private String merchantServiceBaseUrl;

    @Value("${payflow.api-key-cache.internal-endpoint.path:/v1/internal/merchants/validate-key}")
    private String validateKeyPath;

    public JdbcApiKeyAuthenticator(
            JdbcTemplate jdbcTemplate,
            ApiKeyCache apiKeyCache,
            RestTemplate merchantServiceRestTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.apiKeyCache = apiKeyCache;
        this.restTemplate = merchantServiceRestTemplate;
    }

    /**
     * @param rawApiKey Bearer token value (no "Bearer " prefix)
     * @return merchant id when valid; empty when unknown or invalid
     */
    public Optional<MerchantId> resolveMerchantId(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < KEY_PREFIX_LENGTH) {
            return Optional.empty();
        }

        String keyPrefix = rawApiKey.substring(0, KEY_PREFIX_LENGTH);

        // 1. Check cache first
        Optional<ValidatedMerchant> cached = apiKeyCache.get(keyPrefix);
        if (cached.isPresent()) {
            ValidatedMerchant vm = cached.get();
            // Re-validate with BCrypt (cached key hash)
            if (vm.isActive() && bcrypt.matches(rawApiKey, vm.keyHash())) {
                log.debug("Cache hit for key prefix: {}", keyPrefix);
                return Optional.of(MerchantId.of(vm.merchantId().toString()));
            } else {
                // Cached entry is invalid (deactivated or BCrypt mismatch)
                apiKeyCache.evict(keyPrefix);
            }
        }

        // 2. Cache miss - call merchant-service via HTTP, fallback to direct DB
        try {
            Map<String, Object> request = Map.of("keyPrefix", keyPrefix);
            String url = merchantServiceBaseUrl + validateKeyPath;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null) {
                Boolean isActive = (Boolean) response.get("isActive");
                String merchantId = (String) response.get("merchantId");
                String keyHash = (String) response.get("keyHash");

                if (isActive != null && isActive && merchantId != null && keyHash != null) {
                    // Cache the result
                    ValidatedMerchant validated = new ValidatedMerchant(
                            UUID.fromString(merchantId),
                            keyHash,
                            true,
                            Instant.now()
                    );
                    apiKeyCache.put(keyPrefix, validated);

                    // Validate the provided key against the hash
                    if (bcrypt.matches(rawApiKey, keyHash)) {
                        return Optional.of(MerchantId.of(merchantId));
                    }
                } else if (isActive != null && !isActive) {
                    // Merchant exists but is inactive - cache the inactive state
                    ValidatedMerchant validated = new ValidatedMerchant(
                            merchantId != null ? UUID.fromString(merchantId) : UUID.fromString("00000000-0000-0000-0000-000000000000"),
                            keyHash != null ? keyHash : "",
                            false,
                            Instant.now()
                    );
                    apiKeyCache.put(keyPrefix, validated);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to validate key via merchant-service, falling back to direct DB: {}", e.getMessage());
            // Fall back to direct DB query (Phase 8 behavior)
        }

        // 3. Fallback: direct DB query (Phase 8 behavior)
        return jdbcTemplate.query(
                "SELECT id, key_hash FROM merchants.merchants WHERE key_prefix = ? AND is_active = TRUE",
                ps -> ps.setString(1, keyPrefix),
                rs -> {
                    while (rs.next()) {
                        if (bcrypt.matches(rawApiKey, rs.getString("key_hash"))) {
                            return Optional.of(MerchantId.of(rs.getString("id")));
                        }
                    }
                    return Optional.empty();
                }
        );
    }
}