package com.payflow.payment.application.port;

import com.payflow.payment.application.pagination.PageRequest;
import com.payflow.payment.application.pagination.PageResult;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import java.util.Optional;

public interface PaymentRepository {

    void insert(Payment payment, String clientSecret);

    void update(Payment payment);

    Optional<Payment> findByIdAndMerchantId(PaymentId id, MerchantId merchantId);

    PageResult<Payment> findByMerchantId(MerchantId merchantId, Optional<PaymentStatus> statusFilter, PageRequest pageRequest);
}
