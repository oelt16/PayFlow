package com.payflow.payment.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for idempotency keys.
 */
@Repository
public interface IdempotencyKeySpringDataRepository extends JpaRepository<IdempotencyKeyJpaEntity, String> {

    /**
     * Find idempotency key by merchant ID and key.
     */
    Optional<IdempotencyKeyJpaEntity> findByMerchantIdAndKey(String merchantId, String key);

    /**
     * Find all non-expired idempotency keys for a merchant.
     */
    List<IdempotencyKeyJpaEntity> findByMerchantIdAndExpiresAtAfter(String merchantId, Instant timestamp);

    /**
     * Delete all expired idempotency keys. Returns the count of deleted rows.
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKeyJpaEntity e WHERE e.expiresAt < :timestamp")
    long deleteByExpiresAtBefore(@Param("timestamp") Instant timestamp);
}