package com.payflow.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.payflow.payment.application.exception.IdempotencyKeyReuseException;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity;
import com.payflow.payment.infrastructure.persistence.jpa.IdempotencyKeySpringDataRepository;

/**
 * Service for managing idempotency keys.
 * Provides lookup and storage of idempotent request results.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeySpringDataRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final Duration ttl;

    public IdempotencyService(
            IdempotencyKeySpringDataRepository repository,
            TransactionTemplate transactionTemplate,
            Duration ttl
    ) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
        this.ttl = ttl;
    }

    /**
     * Computes SHA-256 hash of the request body.
     *
     * @param body the request body bytes
     * @return lowercase hex string (64 characters)
     */
    public String computeHash(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(body);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Looks up an idempotency key for the given merchant and key.
     *
     * @param merchantId the merchant ID
     * @param key the idempotency key
     * @param hash the request hash to match
     * @return the cached result if found and hash matches, empty if not found,
     *         throws if found but hash doesn't match
     */
    public Optional<IdempotencyResult> lookup(MerchantId merchantId, String key, String hash) {
        Optional<IdempotencyKeyJpaEntity> existing = repository.findByMerchantIdAndKey(
                merchantId.value(),
                key
        );

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyKeyJpaEntity entity = existing.get();

        // Hash mismatch - this is a client error
        if (!entity.getRequestHash().equals(hash)) {
            throw new IdempotencyKeyReuseException(key, entity.getRequestHash(), hash);
        }

        return Optional.of(new IdempotencyResult(
                new LinkedHashMap<>(entity.getResponseBody()),
                entity.getHttpStatus()
        ));
    }

    /**
     * Stores an idempotency key with the response.
     * Uses a transaction to ensure atomicity.
     *
     * @param merchantId the merchant ID
     * @param key the idempotency key
     * @param hash the request hash
     * @param responseBody the response body to cache
     * @param httpStatus the HTTP status code
     */
    public void store(
            MerchantId merchantId,
            String key,
            String hash,
            Map<String, Object> responseBody,
            int httpStatus
    ) {
        transactionTemplate.execute(status -> {
            IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
            entity.setKey(key);
            entity.setMerchantId(merchantId.value());
            entity.setRequestHash(hash);
            entity.setResponseBody(new LinkedHashMap<>(responseBody));
            entity.setHttpStatus(httpStatus);
            entity.setCreatedAt(Instant.now());
            entity.setExpiresAt(Instant.now().plus(ttl));

            repository.save(entity);
            return null;
        });
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}