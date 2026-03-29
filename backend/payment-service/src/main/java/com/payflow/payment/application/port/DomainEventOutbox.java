package com.payflow.payment.application.port;

import com.payflow.payment.domain.DomainEvent;

import java.util.List;

public interface DomainEventOutbox {

    void append(String aggregateId, List<DomainEvent> events);
}
