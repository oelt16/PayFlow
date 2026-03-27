package com.payflow.payment.domain.event;

import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.RefundId;

import java.time.Instant;

public record PaymentRefundedEvent(
        Instant occurredAt,
        PaymentId paymentId,
        RefundId refundId,
        Money refundAmount,
        Money remainingAmount,
        boolean fullRefund
) implements DomainEvent {
}
