package com.payflow.payment.infrastructure.persistence.jpa;

import com.payflow.payment.application.port.RefundRepository;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.Refund;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class JpaRefundRepositoryAdapter implements RefundRepository {

    private final RefundSpringDataRepository springDataRepository;
    private final RefundPersistenceMapper mapper;

    public JpaRefundRepositoryAdapter(
            RefundSpringDataRepository springDataRepository,
            RefundPersistenceMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void insert(Refund refund) {
        springDataRepository.save(mapper.toEntity(refund));
    }

    @Override
    public List<Refund> findByPaymentId(PaymentId paymentId, String currency) {
        return springDataRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId.value()).stream()
                .map(e -> mapper.toDomain(e, currency))
                .toList();
    }
}
