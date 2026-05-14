package com.payflow.payment.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for IdempotencyKeyJpaEntity.
 * Tests the entity field mappings and structure.
 */
@DisplayName("IdempotencyKeyJpaEntity")
class IdempotencyKeyJpaEntityTest {

    private IdempotencyKeyJpaEntity entity;

    @BeforeEach
    void setUp() {
        entity = new IdempotencyKeyJpaEntity();
    }

    @Nested
    @DisplayName("Field Storage and Retrieval")
    class FieldStorage {

        @Test
        @DisplayName("should store and retrieve key")
        void storesAndRetrievesKey() {
            String key = " idempotency-key-123 ";
            entity.setKey(key);
            assertEquals(key, entity.getKey());
        }

        @Test
        @DisplayName("should store and retrieve merchant ID")
        void storesAndRetrievesMerchantId() {
            String merchantId = "merchant-uuid-456";
            entity.setMerchantId(merchantId);
            assertEquals(merchantId, entity.getMerchantId());
        }

        @Test
        @DisplayName("should store and retrieve request hash")
        void storesAndRetrievesRequestHash() {
            String hash = "abc123def456789012345678901234567890123456789012345678901234";
            entity.setRequestHash(hash);
            assertEquals(hash, entity.getRequestHash());
        }

        @Test
        @DisplayName("should store and retrieve HTTP status")
        void storesAndRetrievesHttpStatus() {
            int status = 201;
            entity.setHttpStatus(status);
            assertEquals(status, entity.getHttpStatus());
        }

        @Test
        @DisplayName("should store and retrieve created at timestamp")
        void storesAndRetrievesCreatedAt() {
            Instant createdAt = Instant.parse("2026-05-13T10:00:00Z");
            entity.setCreatedAt(createdAt);
            assertEquals(createdAt, entity.getCreatedAt());
        }

        @Test
        @DisplayName("should store and retrieves expires at timestamp")
        void storesAndRetrievesExpiresAt() {
            Instant expiresAt = Instant.parse("2026-05-14T10:00:00Z");
            entity.setExpiresAt(expiresAt);
            assertEquals(expiresAt, entity.getExpiresAt());
        }
    }

    @Nested
    @DisplayName("JSONB Response Body")
    class ResponseBody {

        @Test
        @DisplayName("should store and retrieve JSONB response body as map")
        void storesAndRetrievesJsonResponseBody() {
            Map<String, Object> responseBody = new LinkedHashMap<>();
            responseBody.put("id", "pay_123");
            responseBody.put("status", "completed");
            responseBody.put("amount", 1000);

            entity.setResponseBody(responseBody);
            assertNotNull(entity.getResponseBody());
            assertEquals("pay_123", entity.getResponseBody().get("id"));
            assertEquals("completed", entity.getResponseBody().get("status"));
        }

        @Test
        @DisplayName("should handle empty response body")
        void handlesEmptyResponseBody() {
            Map<String, Object> emptyBody = new LinkedHashMap<>();
            entity.setResponseBody(emptyBody);
            assertNotNull(entity.getResponseBody());
            assertEquals(0, entity.getResponseBody().size());
        }
    }
}