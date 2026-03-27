package com.payflow.payment.domain;

import com.payflow.payment.domain.event.PaymentCancelledEvent;
import com.payflow.payment.domain.event.PaymentCapturedEvent;
import com.payflow.payment.domain.event.PaymentCreatedEvent;
import com.payflow.payment.domain.event.PaymentExpiredEvent;
import com.payflow.payment.domain.event.PaymentRefundedEvent;
import com.payflow.payment.domain.exception.InsufficientRefundableAmountException;
import com.payflow.payment.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Payment {

    private static final Duration DEFAULT_PENDING_TTL = Duration.ofHours(1);

    private final PaymentId id;
    private final MerchantId merchantId;
    private final Money amount;
    private final String description;
    private final CardDetails cardDetails;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private final Instant expiresAt;

    private PaymentStatus status;
    private Instant capturedAt;
    private Instant cancelledAt;
    private BigDecimal totalRefunded;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Payment(
            PaymentId id,
            MerchantId merchantId,
            Money amount,
            String description,
            CardDetails cardDetails,
            Map<String, String> metadata,
            Instant createdAt,
            Instant expiresAt,
            PaymentStatus status
    ) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.description = description;
        this.cardDetails = cardDetails;
        this.metadata = Map.copyOf(metadata);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.totalRefunded = BigDecimal.ZERO.setScale(2);
    }

    public static Payment create(
            MerchantId merchantId,
            Money amount,
            String description,
            CardDetails cardDetails,
            Map<String, String> metadata,
            Instant createdAt
    ) {
        return create(merchantId, amount, description, cardDetails, metadata, createdAt, DEFAULT_PENDING_TTL);
    }

    public static Payment create(
            MerchantId merchantId,
            Money amount,
            String description,
            CardDetails cardDetails,
            Map<String, String> metadata,
            Instant createdAt,
            Duration pendingTimeToLive
    ) {
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(cardDetails, "cardDetails");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(pendingTimeToLive, "pendingTimeToLive");

        PaymentId id = PaymentId.generate();
        Instant expiresAt = createdAt.plus(pendingTimeToLive);
        Payment payment = new Payment(
                id,
                merchantId,
                amount,
                description,
                cardDetails,
                metadata,
                createdAt,
                expiresAt,
                PaymentStatus.PENDING
        );
        payment.recordEvent(new PaymentCreatedEvent(createdAt, id, merchantId, amount, PaymentStatus.PENDING));
        return payment;
    }

    public void capture(Instant now) {
        Objects.requireNonNull(now, "now");
        ensurePending();
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = now;
        recordEvent(new PaymentCapturedEvent(now, id, merchantId, amount, capturedAt));
    }

    public void cancel(Instant now, Optional<String> reason) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(reason, "reason");
        ensurePending();
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = now;
        recordEvent(new PaymentCancelledEvent(now, id, merchantId, cancelledAt, reason));
    }

    public void cancel(Instant now) {
        cancel(now, Optional.empty());
    }

    public void refund(Money refundAmount, Instant now) {
        Objects.requireNonNull(refundAmount, "refundAmount");
        Objects.requireNonNull(now, "now");
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.PARTIAL_REFUND) {
            throw new InvalidStateTransitionException("Refund not allowed in state: " + status);
        }
        if (!refundAmount.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Refund currency must match payment currency");
        }
        Money refundable = remainingRefundable();
        if (refundAmount.isGreaterThan(refundable)) {
            throw new InsufficientRefundableAmountException(
                    "Refund " + refundAmount + " exceeds refundable " + refundable
            );
        }

        RefundId refundId = RefundId.generate();
        BigDecimal newTotal = totalRefunded.add(refundAmount.amount());
        totalRefunded = newTotal.setScale(2, java.math.RoundingMode.HALF_UP);
        Money remaining = amount.subtract(Money.of(totalRefunded, amount.currency()));
        boolean fullRefund = remaining.amount().compareTo(BigDecimal.ZERO) == 0;
        this.status = fullRefund ? PaymentStatus.REFUNDED : PaymentStatus.PARTIAL_REFUND;
        recordEvent(new PaymentRefundedEvent(now, id, refundId, refundAmount, remaining, fullRefund));
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now");
        ensurePending();
        if (now.isBefore(expiresAt)) {
            throw new InvalidStateTransitionException("Payment cannot expire before expiresAt");
        }
        this.status = PaymentStatus.EXPIRED;
        recordEvent(new PaymentExpiredEvent(now, id, merchantId, now));
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public PaymentId id() {
        return id;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public Money amount() {
        return amount;
    }

    public String description() {
        return description;
    }

    public CardDetails cardDetails() {
        return cardDetails;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public PaymentStatus status() {
        return status;
    }

    public Optional<Instant> capturedAt() {
        return Optional.ofNullable(capturedAt);
    }

    public Optional<Instant> cancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    private void ensurePending() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidStateTransitionException("Expected PENDING, was " + status);
        }
    }

    private Money remainingRefundable() {
        return amount.subtract(Money.of(totalRefunded, amount.currency()));
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}
