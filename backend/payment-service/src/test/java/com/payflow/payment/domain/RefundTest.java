package com.payflow.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundTest {

    @Test
    void holdsFields() {
        RefundId id = RefundId.of("re_abc");
        PaymentId pid = PaymentId.of("pay_xyz");
        Money amount = Money.of(new BigDecimal("10.00"), "USD");
        Instant at = Instant.parse("2025-01-01T12:00:00Z");
        Refund r = new Refund(id, pid, amount, Optional.of("duplicate"), at);
        assertThat(r.id()).isEqualTo(id);
        assertThat(r.paymentId()).isEqualTo(pid);
        assertThat(r.amount()).isEqualTo(amount);
        assertThat(r.reason()).contains("duplicate");
        assertThat(r.createdAt()).isEqualTo(at);
    }
}
