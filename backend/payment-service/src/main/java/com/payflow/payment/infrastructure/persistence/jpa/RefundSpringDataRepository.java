package com.payflow.payment.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundSpringDataRepository extends JpaRepository<RefundJpaEntity, String> {

    List<RefundJpaEntity> findByPaymentIdOrderByCreatedAtAsc(String paymentId);
}
