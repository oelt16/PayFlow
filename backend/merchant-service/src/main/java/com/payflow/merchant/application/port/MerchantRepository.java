package com.payflow.merchant.application.port;

import com.payflow.merchant.domain.Merchant;
import com.payflow.merchant.domain.MerchantId;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository {

    void insert(Merchant merchant);

    Optional<Merchant> findById(MerchantId id);

    List<Merchant> findByKeyPrefix(String keyPrefix);

    boolean existsByEmail(String email);

    void update(Merchant merchant);
}
