package com.payflow.payment.domain;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
