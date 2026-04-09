package com.payflow.merchant.domain.event;

import com.payflow.merchant.domain.DomainEvent;
import com.payflow.merchant.domain.MerchantId;

import java.time.Instant;

public record MerchantCreatedEvent(
        Instant occurredAt,
        MerchantId merchantId,
        String name,
        String email,
        Instant createdAt
) implements DomainEvent {
}
