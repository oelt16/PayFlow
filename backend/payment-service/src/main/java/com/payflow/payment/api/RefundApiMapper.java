package com.payflow.payment.api;

import com.payflow.payment.api.dto.RefundResponse;
import com.payflow.payment.application.money.MoneyMinorUnits;
import com.payflow.payment.domain.Refund;

public final class RefundApiMapper {

    private RefundApiMapper() {
    }

    public static RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.id().value(),
                refund.paymentId().value(),
                MoneyMinorUnits.toMinorUnits(refund.amount()),
                refund.amount().currency(),
                refund.reason().orElse(null),
                refund.createdAt().toString()
        );
    }
}
