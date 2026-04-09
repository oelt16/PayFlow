package com.payflow.merchant.domain;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
