package com.payflow.payment.domain.event;

import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import java.time.Instant;

public record PaymentCreatedEvent(
        Instant occurredAt,
        PaymentId paymentId,
        MerchantId merchantId,
        Money amount,
        PaymentStatus status
) implements DomainEvent {
}
