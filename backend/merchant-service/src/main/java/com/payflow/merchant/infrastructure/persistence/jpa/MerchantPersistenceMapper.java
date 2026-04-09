package com.payflow.merchant.infrastructure.persistence.jpa;

import com.payflow.merchant.domain.ApiKeyHash;
import com.payflow.merchant.domain.Merchant;
import com.payflow.merchant.domain.MerchantId;

import org.springframework.stereotype.Component;

@Component
public class MerchantPersistenceMapper {

    public MerchantJpaEntity toNewEntity(Merchant merchant) {
        MerchantJpaEntity e = new MerchantJpaEntity();
        mergeFromDomain(merchant, e);
        return e;
    }

    public void mergeFromDomain(Merchant merchant, MerchantJpaEntity entity) {
        entity.setId(merchant.id().value());
        entity.setName(merchant.name());
        entity.setEmail(merchant.email());
        entity.setKeyPrefix(merchant.keyPrefix());
        entity.setKeyHash(merchant.keyHash().value());
        entity.setActive(merchant.isActive());
        entity.setCreatedAt(merchant.createdAt());
        entity.setDeactivatedAt(merchant.deactivatedAt());
    }

    public Merchant toDomain(MerchantJpaEntity entity) {
        return Merchant.restore(
                MerchantId.of(entity.getId()),
                entity.getName(),
                entity.getEmail(),
                entity.getKeyPrefix(),
                ApiKeyHash.of(entity.getKeyHash()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getDeactivatedAt()
        );
    }
}
