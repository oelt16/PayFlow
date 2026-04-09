package com.payflow.payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Persisted refund line item for a payment (not the aggregate root).
 */
public final class Refund {

    private final RefundId id;
    private final PaymentId paymentId;
    private final Money amount;
    private final Optional<String> reason;
    private final Instant createdAt;

    public Refund(
            RefundId id,
            PaymentId paymentId,
            Money amount,
            Optional<String> reason,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public RefundId id() {
        return id;
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public Money amount() {
        return amount;
    }

    public Optional<String> reason() {
        return reason;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
