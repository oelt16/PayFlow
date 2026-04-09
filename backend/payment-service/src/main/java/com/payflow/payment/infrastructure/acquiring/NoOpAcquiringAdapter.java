package com.payflow.payment.infrastructure.acquiring;

import com.payflow.payment.application.port.AcquiringPort;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;

import org.springframework.stereotype.Component;

@Component
public class NoOpAcquiringAdapter implements AcquiringPort {

    @Override
    public void confirmCapture(PaymentId paymentId, Money amount, MerchantId merchantId) {
        // Simulated acquirer: always succeeds (no network).
    }

    @Override
    public void confirmRefund(PaymentId paymentId, Money refundAmount, MerchantId merchantId) {
        // Simulated acquirer: always succeeds (no network).
    }
}
