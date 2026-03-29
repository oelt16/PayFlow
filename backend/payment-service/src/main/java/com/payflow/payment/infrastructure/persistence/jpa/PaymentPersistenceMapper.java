package com.payflow.payment.infrastructure.persistence.jpa;

import com.payflow.payment.domain.CardDetails;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public PaymentJpaEntity toNewEntity(Payment payment, String clientSecret) {
        PaymentJpaEntity e = new PaymentJpaEntity();
        copyFromDomain(payment, e, clientSecret);
        return e;
    }

    public void mergeFromDomain(Payment payment, PaymentJpaEntity target) {
        copyFromDomain(payment, target, target.getClientSecret());
    }

    public Payment toDomain(PaymentJpaEntity e) {
        CardDetails card = new CardDetails(
                e.getCardLast4(),
                e.getCardBrand(),
                e.getCardExpMonth(),
                e.getCardExpYear()
        );
        return Payment.restore(
                PaymentId.of(e.getId()),
                MerchantId.of(e.getMerchantId()),
                Money.of(e.getAmount(), e.getCurrency()),
                e.getDescription(),
                card,
                e.getMetadata(),
                e.getCreatedAt(),
                e.getExpiresAt(),
                e.getStatus(),
                e.getCapturedAt(),
                e.getCancelledAt(),
                e.getTotalRefunded()
        );
    }

    private static void copyFromDomain(Payment payment, PaymentJpaEntity e, String clientSecret) {
        e.setId(payment.id().value());
        e.setMerchantId(payment.merchantId().value());
        e.setAmount(payment.amount().amount());
        e.setCurrency(payment.amount().currency());
        e.setStatus(payment.status());
        e.setDescription(payment.description());
        CardDetails c = payment.cardDetails();
        e.setCardLast4(c.last4());
        e.setCardBrand(c.brand());
        e.setCardExpMonth(c.expiryMonth());
        e.setCardExpYear(c.expiryYear());
        e.setMetadata(payment.metadata());
        e.setClientSecret(clientSecret);
        e.setTotalRefunded(payment.totalRefundedAmount());
        e.setCreatedAt(payment.createdAt());
        e.setCapturedAt(payment.capturedAt().orElse(null));
        e.setCancelledAt(payment.cancelledAt().orElse(null));
        e.setExpiresAt(payment.expiresAt());
    }
}
