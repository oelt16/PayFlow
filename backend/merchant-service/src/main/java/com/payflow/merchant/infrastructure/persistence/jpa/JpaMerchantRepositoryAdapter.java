package com.payflow.merchant.infrastructure.persistence.jpa;

import com.payflow.merchant.application.port.MerchantRepository;
import com.payflow.merchant.domain.Merchant;
import com.payflow.merchant.domain.MerchantId;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class JpaMerchantRepositoryAdapter implements MerchantRepository {

    private final MerchantSpringDataRepository springDataRepository;
    private final MerchantPersistenceMapper mapper;

    public JpaMerchantRepositoryAdapter(
            MerchantSpringDataRepository springDataRepository,
            MerchantPersistenceMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void insert(Merchant merchant) {
        springDataRepository.save(mapper.toNewEntity(merchant));
    }

    @Override
    public Optional<Merchant> findById(MerchantId id) {
        return springDataRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Merchant> findByKeyPrefix(String keyPrefix) {
        return springDataRepository.findByKeyPrefix(keyPrefix).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public void update(Merchant merchant) {
        MerchantJpaEntity entity = springDataRepository
                .findById(merchant.id().value())
                .orElseThrow(() -> new IllegalStateException("Merchant not found for update: " + merchant.id().value()));
        mapper.mergeFromDomain(merchant, entity);
        springDataRepository.save(entity);
    }
}
