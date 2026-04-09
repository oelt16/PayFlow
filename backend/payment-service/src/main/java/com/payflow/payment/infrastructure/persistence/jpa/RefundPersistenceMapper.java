package com.payflow.payment.infrastructure.persistence.jpa;

import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.Refund;
import com.payflow.payment.domain.RefundId;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class RefundPersistenceMapper {

    public RefundJpaEntity toEntity(Refund refund) {
        RefundJpaEntity e = new RefundJpaEntity();
        e.setId(refund.id().value());
        e.setPaymentId(refund.paymentId().value());
        e.setAmount(refund.amount().amount());
        e.setReason(refund.reason().orElse(null));
        e.setCreatedAt(refund.createdAt());
        return e;
    }

    public Refund toDomain(RefundJpaEntity e, String currency) {
        return new Refund(
                RefundId.of(e.getId()),
                PaymentId.of(e.getPaymentId()),
                Money.of(e.getAmount(), currency),
                Optional.ofNullable(e.getReason()).filter(s -> !s.isEmpty()),
                e.getCreatedAt()
        );
    }
}
