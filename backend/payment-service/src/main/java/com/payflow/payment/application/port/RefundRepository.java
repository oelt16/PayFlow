package com.payflow.payment.application.port;

import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.Refund;

import java.util.List;

public interface RefundRepository {

    void insert(Refund refund);

    List<Refund> findByPaymentId(PaymentId paymentId, String currency);
}
