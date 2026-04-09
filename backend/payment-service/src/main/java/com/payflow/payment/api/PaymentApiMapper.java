package com.payflow.payment.api;

import com.payflow.payment.api.dto.CardResponse;
import com.payflow.payment.api.dto.PaymentResponse;
import com.payflow.payment.application.money.MoneyMinorUnits;
import com.payflow.payment.domain.CardDetails;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.Payment;

import java.time.Instant;

public final class PaymentApiMapper {

    private PaymentApiMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null);
    }

    public static PaymentResponse toResponse(Payment payment, String clientSecret) {
        CardDetails c = payment.cardDetails();
        CardResponse card = new CardResponse(c.last4(), c.brand().name(), c.expiryMonth(), c.expiryYear());
        long totalRefundedMinor = MoneyMinorUnits.toMinorUnits(
                Money.of(payment.totalRefundedAmount(), payment.amount().currency())
        );
        return new PaymentResponse(
                payment.id().value(),
                MoneyMinorUnits.toMinorUnits(payment.amount()),
                payment.amount().currency(),
                payment.status().name(),
                payment.description(),
                clientSecret,
                payment.metadata(),
                card,
                payment.createdAt().toString(),
                payment.expiresAt().toString(),
                payment.capturedAt().map(Instant::toString).orElse(null),
                payment.cancelledAt().map(Instant::toString).orElse(null),
                totalRefundedMinor,
                totalRefundedMinor
        );
    }
}
