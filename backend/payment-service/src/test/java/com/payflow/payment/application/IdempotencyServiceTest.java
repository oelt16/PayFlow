package com.payflow.payment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payflow.payment.application.exception.IdempotencyKeyReuseException;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity;
import com.payflow.payment.infrastructure.persistence.jpa.IdempotencyKeySpringDataRepository;

/**
 * Unit tests for IdempotencyService.
 * Tests computeHash and lookup - store is tested via integration tests.
 */
@DisplayName("IdempotencyService")
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final MerchantId MERCHANT_ID = MerchantId.of("merchant-123");
    private static final String IDEMPOTENCY_KEY = "idempotency-key-abc";
    private static final String REQUEST_HASH = "abc123def456789012345678901234567890123456789012345678901234";
    private static final String DIFFERENT_HASH = "xyz789abcdef01234567890123456789012345678901234567890123456";

    @Mock
    private IdempotencyKeySpringDataRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        // Use a minimal mock for TransactionTemplate that just executes the callback
        // This works because we're not testing the store method in unit tests
        var txTemplate = new org.springframework.transaction.support.TransactionTemplate();
        service = new IdempotencyService(repository, txTemplate, Duration.ofHours(24));
    }

    private Map<String, Object> createResponseBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", "pay_123");
        body.put("status", "completed");
        body.put("amount", 1000);
        return body;
    }

    @Nested
    @DisplayName("computeHash")
    class ComputeHash {

        @Test
        @DisplayName("should compute SHA-256 hash as lowercase hex string")
        void computesSha256Hash() {
            byte[] body = "{\"test\":\"data\"}".getBytes();

            String hash = service.computeHash(body);

            assertNotNull(hash);
            assertEquals(64, hash.length());
            // SHA-256 produces lowercase hex
            assertEquals(hash, hash.toLowerCase());
        }

        @Test
        @DisplayName("should produce same hash for same input")
        void producesSameHashForSameInput() {
            byte[] body = "same body".getBytes();

            String hash1 = service.computeHash(body);
            String hash2 = service.computeHash(body);

            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("should produce different hash for different input")
        void producesDifferentHashForDifferentInput() {
            byte[] body1 = "body one".getBytes();
            byte[] body2 = "body two".getBytes();

            String hash1 = service.computeHash(body1);
            String hash2 = service.computeHash(body2);

            assertFalse(hash1.equals(hash2));
        }

        @Test
        @DisplayName("should handle empty body")
        void handlesEmptyBody() {
            byte[] body = new byte[0];

            String hash = service.computeHash(body);

            assertEquals(64, hash.length());
        }

        @Test
        @DisplayName("should produce 64 character hash for typical JSON")
        void produces64CharHashForTypicalJson() {
            byte[] body = "{\"amount\":1000,\"currency\":\"USD\",\"payment_method\":\"card\"}".getBytes();

            String hash = service.computeHash(body);

            assertEquals(64, hash.length());
            // Verify it's valid hex
            assertTrue(hash.matches("[0-9a-f]+"));
        }
    }

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @Test
        @DisplayName("should return empty when no key found")
        void returnsEmptyWhenNotFound() {
            when(repository.findByMerchantIdAndKey(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            Optional<IdempotencyResult> result = service.lookup(MERCHANT_ID, IDEMPOTENCY_KEY, REQUEST_HASH);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return result when key found with matching hash")
        void returnsResultWhenFoundWithMatchingHash() {
            IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
            entity.setKey(IDEMPOTENCY_KEY);
            entity.setMerchantId(MERCHANT_ID.value());
            entity.setRequestHash(REQUEST_HASH);
            entity.setResponseBody(createResponseBody());
            entity.setHttpStatus(201);

            when(repository.findByMerchantIdAndKey(MERCHANT_ID.value(), IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(entity));

            Optional<IdempotencyResult> result = service.lookup(MERCHANT_ID, IDEMPOTENCY_KEY, REQUEST_HASH);

            assertTrue(result.isPresent());
            assertEquals(201, result.get().httpStatus());
            assertEquals("pay_123", result.get().responseBody().get("id"));
        }

        @Test
        @DisplayName("should throw when key found with different hash")
        void throwsWhenFoundWithDifferentHash() {
            IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
            entity.setKey(IDEMPOTENCY_KEY);
            entity.setMerchantId(MERCHANT_ID.value());
            entity.setRequestHash(REQUEST_HASH);

            when(repository.findByMerchantIdAndKey(MERCHANT_ID.value(), IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(entity));

            try {
                service.lookup(MERCHANT_ID, IDEMPOTENCY_KEY, DIFFERENT_HASH);
            } catch (IdempotencyKeyReuseException ex) {
                assertEquals(IDEMPOTENCY_KEY, ex.getKey());
                assertEquals(REQUEST_HASH, ex.getStoredHash());
                assertEquals(DIFFERENT_HASH, ex.getNewHash());
                return;
            }
            throw new AssertionError("Expected IdempotencyKeyReuseException");
        }

        @Test
        @DisplayName("should return result with correct HTTP status")
        void returnsResultWithCorrectHttpStatus() {
            IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
            entity.setKey(IDEMPOTENCY_KEY);
            entity.setMerchantId(MERCHANT_ID.value());
            entity.setRequestHash(REQUEST_HASH);
            entity.setResponseBody(createResponseBody());
            entity.setHttpStatus(400);

            when(repository.findByMerchantIdAndKey(MERCHANT_ID.value(), IDEMPOTENCY_KEY))
                    .thenReturn(Optional.of(entity));

            Optional<IdempotencyResult> result = service.lookup(MERCHANT_ID, IDEMPOTENCY_KEY, REQUEST_HASH);

            assertTrue(result.isPresent());
            assertEquals(400, result.get().httpStatus());
        }
    }
}