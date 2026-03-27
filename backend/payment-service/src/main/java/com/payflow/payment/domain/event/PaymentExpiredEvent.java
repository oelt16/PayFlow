package com.payflow.payment.domain.event;

import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.PaymentId;

import java.time.Instant;

public record PaymentExpiredEvent(
        Instant occurredAt,
        PaymentId paymentId,
        MerchantId merchantId,
        Instant expiredAt
) implements DomainEvent {
}
