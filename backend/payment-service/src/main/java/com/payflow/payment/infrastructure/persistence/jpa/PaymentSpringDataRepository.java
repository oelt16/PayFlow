package com.payflow.payment.infrastructure.persistence.jpa;

import com.payflow.payment.domain.PaymentStatus;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSpringDataRepository extends JpaRepository<PaymentJpaEntity, String> {

    Optional<PaymentJpaEntity> findByIdAndMerchantId(String id, String merchantId);

    Page<PaymentJpaEntity> findByMerchantId(String merchantId, Pageable pageable);

    Page<PaymentJpaEntity> findByMerchantIdAndStatus(String merchantId, PaymentStatus status, Pageable pageable);
}
