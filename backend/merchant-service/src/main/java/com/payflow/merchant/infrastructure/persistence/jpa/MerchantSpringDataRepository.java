package com.payflow.merchant.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantSpringDataRepository extends JpaRepository<MerchantJpaEntity, String> {

    List<MerchantJpaEntity> findByKeyPrefix(String keyPrefix);

    boolean existsByEmail(String email);
}
