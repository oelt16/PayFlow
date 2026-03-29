package com.payflow.payment.infrastructure.persistence.jpa;

import com.payflow.payment.application.pagination.PageRequest;
import com.payflow.payment.application.pagination.PageResult;
import com.payflow.payment.application.port.PaymentRepository;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentSpringDataRepository springDataRepository;
    private final PaymentPersistenceMapper mapper;

    public JpaPaymentRepositoryAdapter(
            PaymentSpringDataRepository springDataRepository,
            PaymentPersistenceMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void insert(Payment payment, String clientSecret) {
        springDataRepository.save(mapper.toNewEntity(payment, clientSecret));
    }

    @Override
    public void update(Payment payment) {
        PaymentJpaEntity entity = springDataRepository
                .findById(payment.id().value())
                .orElseThrow(() -> new IllegalStateException("Payment not found for update: " + payment.id().value()));
        mapper.mergeFromDomain(payment, entity);
        springDataRepository.save(entity);
    }

    @Override
    public Optional<Payment> findByIdAndMerchantId(PaymentId id, MerchantId merchantId) {
        return springDataRepository
                .findByIdAndMerchantId(id.value(), merchantId.value())
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<Payment> findByMerchantId(
            MerchantId merchantId,
            Optional<PaymentStatus> statusFilter,
            PageRequest pageRequest
    ) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        Page<PaymentJpaEntity> page = statusFilter
                .map(s -> springDataRepository.findByMerchantIdAndStatus(merchantId.value(), s, pageable))
                .orElseGet(() -> springDataRepository.findByMerchantId(merchantId.value(), pageable));
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }
}
