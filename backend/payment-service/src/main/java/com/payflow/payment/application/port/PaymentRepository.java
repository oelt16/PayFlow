package com.payflow.payment.application.port;

import com.payflow.payment.application.pagination.PageRequest;
import com.payflow.payment.application.pagination.PageResult;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    void insert(Payment payment, String clientSecret);

    void update(Payment payment);

    Optional<Payment> findByIdAndMerchantId(PaymentId id, MerchantId merchantId);

    PageResult<Payment> findByMerchantId(MerchantId merchantId, Optional<PaymentStatus> statusFilter, PageRequest pageRequest);

    /**
     * Find pending payments that are eligible for expiry.
     * Query: status=PENDING AND createdAt < cutoff AND expiresAt <= now
     *
     * @param createdBefore cutoff timestamp for createdAt (payments created before this are eligible)
     * @param expiresAtOrBefore timestamp for expiresAt (payments expiring at or before this are eligible)
     * @param limit maximum number of payments to return
     * @return list of pending payments eligible for expiry
     */
    List<Payment> findPendingOlderThan(Instant createdBefore, Instant expiresAtOrBefore, int limit);
}
