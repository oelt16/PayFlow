package com.payflow.payment.infrastructure.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventSpringDataRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}
