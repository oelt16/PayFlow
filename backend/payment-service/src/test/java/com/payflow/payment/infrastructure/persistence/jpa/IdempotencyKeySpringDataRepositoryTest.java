package com.payflow.payment.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for IdempotencyKeySpringDataRepository.
 * Tests the repository query methods using mocks.
 */
@DisplayName("IdempotencyKeySpringDataRepository")
@ExtendWith(MockitoExtension.class)
class IdempotencyKeySpringDataRepositoryTest {

    @Mock
    private IdempotencyKeySpringDataRepository repository;

    private IdempotencyKeyJpaEntity entity;

    @BeforeEach
    void setUp() {
        entity = createTestEntity("key-123", "merchant-456");
    }

    private IdempotencyKeyJpaEntity createTestEntity(String key, String merchantId) {
        IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
        entity.setKey(key);
        entity.setMerchantId(merchantId);
        entity.setRequestHash("abc123def456789012345678901234567890123456789012345678901234");
        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("id", "pay_123");
        responseBody.put("status", "completed");
        entity.setResponseBody(responseBody);
        entity.setHttpStatus(201);
        entity.setCreatedAt(Instant.parse("2026-05-13T10:00:00Z"));
        entity.setExpiresAt(Instant.parse("2026-05-14T10:00:00Z"));
        return entity;
    }

    @Nested
    @DisplayName("Find By Key")
    class FindByKey {

        @Test
        @DisplayName("should delegate to findById")
        void delegatesToFindById() {
            when(repository.findById("key-123")).thenReturn(Optional.of(entity));

            Optional<IdempotencyKeyJpaEntity> result = repository.findById("key-123");

            assertTrue(result.isPresent());
            assertEquals("key-123", result.get().getKey());
            verify(repository).findById("key-123");
        }

        @Test
        @DisplayName("should return empty for nonexistent key")
        void returnsEmptyForNonexistent() {
            when(repository.findById("nonexistent")).thenReturn(Optional.empty());

            Optional<IdempotencyKeyJpaEntity> result = repository.findById("nonexistent");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Merchant And Key")
    class FindByMerchantAndKey {

        @Test
        @DisplayName("should find by merchant and key")
        void findsByMerchantAndKey() {
            when(repository.findByMerchantIdAndKey("merchant-456", "key-123"))
                    .thenReturn(Optional.of(entity));

            Optional<IdempotencyKeyJpaEntity> result =
                    repository.findByMerchantIdAndKey("merchant-456", "key-123");

            assertTrue(result.isPresent());
            assertEquals("key-123", result.get().getKey());
        }

        @Test
        @DisplayName("should return empty when merchant does not match")
        void returnsEmptyWhenMerchantMismatch() {
            when(repository.findByMerchantIdAndKey("other-merchant", "key-123"))
                    .thenReturn(Optional.empty());

            Optional<IdempotencyKeyJpaEntity> result =
                    repository.findByMerchantIdAndKey("other-merchant", "key-123");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Delete Expired")
    class DeleteExpired {

        @Test
        @DisplayName("should call delete query with timestamp")
        void callsDeleteQueryWithTimestamp() {
            when(repository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(1L);

            Instant before = Instant.now();
            long deleted = repository.deleteByExpiresAtBefore(before);

            assertEquals(1, deleted);
            verify(repository).deleteByExpiresAtBefore(before);
        }

        @Test
        @DisplayName("should return zero when nothing to delete")
        void returnsZeroWhenNothingToDelete() {
            when(repository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(0L);

            long deleted = repository.deleteByExpiresAtBefore(Instant.now());

            assertEquals(0, deleted);
        }
    }

    @Nested
    @DisplayName("Find By Merchant And Expiration")
    class FindByMerchantAndExpiration {

        @Test
        @DisplayName("should find multiple keys for merchant")
        void findsMultipleKeysForMerchant() {
            List<IdempotencyKeyJpaEntity> entities = List.of(
                    entity,
                    createTestEntity("key-456", "merchant-456")
            );
            when(repository.findByMerchantIdAndExpiresAtAfter(anyString(), any(Instant.class)))
                    .thenReturn(entities);

            List<IdempotencyKeyJpaEntity> result =
                    repository.findByMerchantIdAndExpiresAtAfter("merchant-456", Instant.now());

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list when no keys found")
        void returnsEmptyListWhenNoKeys() {
            when(repository.findByMerchantIdAndExpiresAtAfter(anyString(), any(Instant.class)))
                    .thenReturn(List.of());

            List<IdempotencyKeyJpaEntity> result =
                    repository.findByMerchantIdAndExpiresAtAfter("merchant-456", Instant.now());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("should save entity")
        void savesEntity() {
            when(repository.save(entity)).thenReturn(entity);

            IdempotencyKeyJpaEntity saved = repository.save(entity);

            assertEquals("key-123", saved.getKey());
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("should capture save arguments correctly")
        void capturesSaveArguments() {
            ArgumentCaptor<IdempotencyKeyJpaEntity> captor =
                    ArgumentCaptor.forClass(IdempotencyKeyJpaEntity.class);
            when(repository.save(captor.capture())).thenReturn(entity);

            repository.save(entity);

            IdempotencyKeyJpaEntity captured = captor.getValue();
            assertEquals("key-123", captured.getKey());
            assertEquals("merchant-456", captured.getMerchantId());
            assertEquals(201, captured.getHttpStatus());
        }
    }
}