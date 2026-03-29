package com.payflow.payment.application.port;

import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.PaymentId;

public interface AcquiringPort {

    void confirmCapture(PaymentId paymentId, Money amount, MerchantId merchantId);
}
