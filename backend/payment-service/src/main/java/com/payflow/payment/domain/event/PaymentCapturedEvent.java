package com.payflow.payment.domain.event;

import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;

import java.time.Instant;

public record PaymentCapturedEvent(
        Instant occurredAt,
        PaymentId paymentId,
        MerchantId merchantId,
        Money amount,
        Instant capturedAt
) implements DomainEvent {
}
