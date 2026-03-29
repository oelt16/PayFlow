package com.payflow.payment.domain;

import com.payflow.payment.domain.event.PaymentCapturedEvent;
import com.payflow.payment.domain.event.PaymentCancelledEvent;
import com.payflow.payment.domain.event.PaymentCreatedEvent;
import com.payflow.payment.domain.event.PaymentExpiredEvent;
import com.payflow.payment.domain.event.PaymentRefundedEvent;
import com.payflow.payment.domain.exception.InsufficientRefundableAmountException;
import com.payflow.payment.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant T0 = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void createInitialisesPendingAndEmitsCreated() {
        Payment p = newPayment();
        assertThat(p.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.peekDomainEvents()).hasSize(1);
        assertThat(p.peekDomainEvents().getFirst()).isInstanceOf(PaymentCreatedEvent.class);
        PaymentCreatedEvent e = (PaymentCreatedEvent) p.peekDomainEvents().getFirst();
        assertThat(e.paymentId()).isEqualTo(p.id());
        assertThat(e.merchantId()).isEqualTo(p.merchantId());
        assertThat(e.amount()).isEqualTo(p.amount());
        assertThat(e.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void captureFromPendingTransitionsToCaptured() {
        Payment p = newPayment();
        p.pullDomainEvents();
        Instant cap = T0.plusSeconds(5);
        p.capture(cap);
        assertThat(p.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(p.capturedAt()).contains(cap);
        assertThat(p.peekDomainEvents()).singleElement().isInstanceOf(PaymentCapturedEvent.class);
    }

    @Test
    void captureWhenCapturedThrows() {
        Payment p = newPayment();
        p.capture(T0.plusSeconds(1));
        assertThatThrownBy(() -> p.capture(T0.plusSeconds(2)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void refundExceedingTotalThrows() {
        Payment p = capturedPayment();
        assertThatThrownBy(() -> p.refund(Money.of(new BigDecimal("150.00"), "USD"), T0.plusSeconds(10)))
                .isInstanceOf(InsufficientRefundableAmountException.class);
    }

    @Test
    void partialRefundSetsPartialRefundStatus() {
        Payment p = capturedPayment();
        p.pullDomainEvents();
        p.refund(Money.of(new BigDecimal("40.00"), "USD"), T0.plusSeconds(10));
        assertThat(p.status()).isEqualTo(PaymentStatus.PARTIAL_REFUND);
        assertThat(p.peekDomainEvents().getFirst()).isInstanceOf(PaymentRefundedEvent.class);
        PaymentRefundedEvent e = (PaymentRefundedEvent) p.peekDomainEvents().getFirst();
        assertThat(e.fullRefund()).isFalse();
        assertThat(e.remainingAmount()).isEqualTo(Money.of(new BigDecimal("60.00"), "USD"));
    }

    @Test
    void fullRefundSetsRefundedStatus() {
        Payment p = capturedPayment();
        p.pullDomainEvents();
        p.refund(Money.of(new BigDecimal("100.00"), "USD"), T0.plusSeconds(10));
        assertThat(p.status()).isEqualTo(PaymentStatus.REFUNDED);
        PaymentRefundedEvent e = (PaymentRefundedEvent) p.peekDomainEvents().getFirst();
        assertThat(e.fullRefund()).isTrue();
        assertThat(e.remainingAmount()).isEqualTo(Money.of(BigDecimal.ZERO, "USD"));
    }

    @Test
    void expireAfterTtlTransitionsToExpired() {
        Payment p = Payment.create(
                MerchantId.of("mer_test"),
                Money.of(new BigDecimal("10.00"), "USD"),
                "Order",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of(),
                T0,
                Duration.ofHours(1)
        );
        p.pullDomainEvents();
        Instant after = T0.plus(Duration.ofHours(1));
        p.expire(after);
        assertThat(p.status()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(p.peekDomainEvents().getFirst()).isInstanceOf(PaymentExpiredEvent.class);
    }

    @Test
    void expireBeforeTtlThrows() {
        Payment p = newPayment();
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.expire(T0.plus(Duration.ofMinutes(30))))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelFromPending() {
        Payment p = newPayment();
        p.pullDomainEvents();
        p.cancel(T0.plusSeconds(2), Optional.of("user"));
        assertThat(p.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(p.peekDomainEvents().getFirst()).isInstanceOf(PaymentCancelledEvent.class);
    }

    @Test
    void refundFromPendingThrows() {
        Payment p = newPayment();
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.refund(Money.of(new BigDecimal("10.00"), "USD"), T0.plusSeconds(5)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void refundFromCancelledThrows() {
        Payment p = newPayment();
        p.pullDomainEvents();
        p.cancel(T0.plusSeconds(1));
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.refund(Money.of(new BigDecimal("10.00"), "USD"), T0.plusSeconds(5)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelWhenCapturedThrows() {
        Payment p = capturedPayment();
        assertThatThrownBy(() -> p.cancel(T0.plusSeconds(5)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void captureWhenCancelledThrows() {
        Payment p = newPayment();
        p.pullDomainEvents();
        p.cancel(T0.plusSeconds(1));
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.capture(T0.plusSeconds(5)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void secondFullRefundAfterRefundedThrows() {
        Payment p = capturedPayment();
        p.pullDomainEvents();
        p.refund(Money.of(new BigDecimal("100.00"), "USD"), T0.plusSeconds(10));
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.refund(Money.of(BigDecimal.ONE, "USD"), T0.plusSeconds(11)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void pullDomainEventsClearsBuffer() {
        Payment p = newPayment();
        assertThat(p.pullDomainEvents()).hasSize(1);
        assertThat(p.peekDomainEvents()).isEmpty();
    }

    @Test
    void multiplePartialRefundsThenFull() {
        Payment p = capturedPayment();
        p.pullDomainEvents();
        p.refund(Money.of(new BigDecimal("30.00"), "USD"), T0.plusSeconds(10));
        p.pullDomainEvents();
        assertThat(p.status()).isEqualTo(PaymentStatus.PARTIAL_REFUND);
        p.refund(Money.of(new BigDecimal("20.00"), "USD"), T0.plusSeconds(11));
        p.pullDomainEvents();
        assertThat(p.status()).isEqualTo(PaymentStatus.PARTIAL_REFUND);
        p.refund(Money.of(new BigDecimal("50.00"), "USD"), T0.plusSeconds(12));
        assertThat(p.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refundCurrencyMismatchThrows() {
        Payment p = capturedPayment();
        p.pullDomainEvents();
        assertThatThrownBy(() -> p.refund(Money.of(new BigDecimal("10.00"), "EUR"), T0.plusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void accessorsExposeImmutableState() {
        Payment p = newPayment();
        assertThat(p.description()).isEqualTo("Order #1");
        assertThat(p.cardDetails()).isEqualTo(new CardDetails("4242", CardBrand.VISA, 12, 2027));
        assertThat(p.metadata()).containsEntry("orderId", "ORD-789");
        assertThat(p.createdAt()).isEqualTo(T0);
        assertThat(p.expiresAt()).isEqualTo(T0.plus(Duration.ofHours(1)));
        assertThat(p.cancelledAt()).isEmpty();
    }

    @Test
    void cancelledAtAfterCancel() {
        Payment p = newPayment();
        p.pullDomainEvents();
        Instant at = T0.plusSeconds(3);
        p.cancel(at);
        assertThat(p.cancelledAt()).contains(at);
    }

    @Test
    void restoreRehydratesStateWithoutRecordingEvents() {
        PaymentId id = PaymentId.of("pay_restore1");
        Instant captured = T0.plusSeconds(2);
        Payment p = Payment.restore(
                id,
                MerchantId.of("mer_test"),
                Money.of(new BigDecimal("50.00"), "USD"),
                "Restored",
                new CardDetails("1234", CardBrand.MASTERCARD, 6, 2028),
                Collections.emptyMap(),
                T0,
                T0.plus(Duration.ofHours(1)),
                PaymentStatus.CAPTURED,
                captured,
                null,
                BigDecimal.ZERO.setScale(2)
        );
        assertThat(p.id()).isEqualTo(id);
        assertThat(p.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(p.capturedAt()).contains(captured);
        assertThat(p.peekDomainEvents()).isEmpty();
    }

    @Test
    void restoreNullTotalRefundedNormalizesToZeroScale() {
        Payment p = Payment.restore(
                PaymentId.of("pay_restore2"),
                MerchantId.of("mer_test"),
                Money.of(new BigDecimal("10.00"), "USD"),
                null,
                new CardDetails("9999", CardBrand.VISA, 1, 2030),
                Map.of(),
                T0,
                T0.plus(Duration.ofHours(1)),
                PaymentStatus.PENDING,
                null,
                null,
                null
        );
        assertThat(p.totalRefundedAmount()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        assertThat(p.description()).isNull();
    }

    private static Payment newPayment() {
        return Payment.create(
                MerchantId.of("mer_test"),
                Money.of(new BigDecimal("100.00"), "USD"),
                "Order #1",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of("orderId", "ORD-789"),
                T0
        );
    }

    private static Payment capturedPayment() {
        Payment p = newPayment();
        p.pullDomainEvents();
        p.capture(T0.plusSeconds(1));
        return p;
    }
}
