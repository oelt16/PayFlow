package com.payflow.merchant.domain.event;

import com.payflow.merchant.domain.DomainEvent;
import com.payflow.merchant.domain.MerchantId;

import java.time.Instant;

public record MerchantDeactivatedEvent(
        Instant occurredAt,
        MerchantId merchantId,
        Instant deactivatedAt
) implements DomainEvent {
}
