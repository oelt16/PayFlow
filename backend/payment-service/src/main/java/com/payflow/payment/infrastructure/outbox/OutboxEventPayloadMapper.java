package com.payflow.payment.infrastructure.outbox;

import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.event.PaymentCancelledEvent;
import com.payflow.payment.domain.event.PaymentCapturedEvent;
import com.payflow.payment.domain.event.PaymentCreatedEvent;
import com.payflow.payment.domain.event.PaymentRefundedEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class OutboxEventPayloadMapper {

    public String eventType(DomainEvent event) {
        if (event instanceof PaymentCreatedEvent) {
            return "payment.created";
        }
        if (event instanceof PaymentCapturedEvent) {
            return "payment.captured";
        }
        if (event instanceof PaymentCancelledEvent) {
            return "payment.cancelled";
        }
        if (event instanceof PaymentRefundedEvent) {
            return "payment.refunded";
        }
        throw new IllegalArgumentException("Unsupported domain event: " + event.getClass().getName());
    }

    public Map<String, Object> payload(DomainEvent event) {
        if (event instanceof PaymentCreatedEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", e.paymentId().value());
            m.put("merchantId", e.merchantId().value());
            m.put("amount", e.amount().amount());
            m.put("currency", e.amount().currency());
            m.put("status", e.status().name());
            return m;
        }
        if (event instanceof PaymentCapturedEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", e.paymentId().value());
            m.put("merchantId", e.merchantId().value());
            m.put("amount", e.amount().amount());
            m.put("currency", e.amount().currency());
            m.put("capturedAt", e.capturedAt().toString());
            return m;
        }
        if (event instanceof PaymentCancelledEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paymentId", e.paymentId().value());
            m.put("merchantId", e.merchantId().value());
            m.put("cancelledAt", e.cancelledAt().toString());
            e.reason().ifPresent(r -> m.put("reason", r));
            return m;
        }
        if (event instanceof PaymentRefundedEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("merchantId", e.merchantId().value());
            m.put("paymentId", e.paymentId().value());
            m.put("refundId", e.refundId().value());
            m.put("refundAmount", e.refundAmount().amount());
            m.put("remainingAmount", e.remainingAmount().amount());
            m.put("isFullRefund", e.fullRefund());
            return m;
        }
        throw new IllegalArgumentException("Unsupported domain event: " + event.getClass().getName());
    }
}
