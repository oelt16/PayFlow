package com.payflow.payment.application.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for IdempotencyKeyReuseException.
 */
@DisplayName("IdempotencyKeyReuseException")
class IdempotencyKeyReuseExceptionTest {

    @Test
    @DisplayName("should store key and hash mismatch information")
    void storesKeyAndHashMismatch() {
        String key = "idempotency-key-123";
        String expectedHash = "abc123";
        String actualHash = "xyz789";

        IdempotencyKeyReuseException exception =
                new IdempotencyKeyReuseException(key, expectedHash, actualHash);

        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("different request body"));
    }

    @Test
    @DisplayName("should expose the idempotency key")
    void exposesIdempotencyKey() {
        String key = "my-unique-key";

        IdempotencyKeyReuseException exception =
                new IdempotencyKeyReuseException(key, "hash1", "hash2");

        assertEquals(key, exception.getKey());
    }

    @Test
    @DisplayName("should expose the stored hash")
    void exposesStoredHash() {
        String storedHash = "stored-hash-abc";

        IdempotencyKeyReuseException exception =
                new IdempotencyKeyReuseException("key", storedHash, "different-hash");

        assertEquals(storedHash, exception.getStoredHash());
    }

    @Test
    @DisplayName("should expose the new hash")
    void exposesNewHash() {
        String newHash = "new-hash-xyz";

        IdempotencyKeyReuseException exception =
                new IdempotencyKeyReuseException("key", "stored", newHash);

        assertEquals(newHash, exception.getNewHash());
    }
}