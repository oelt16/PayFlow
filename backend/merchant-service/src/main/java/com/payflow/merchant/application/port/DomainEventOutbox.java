package com.payflow.merchant.application.port;

import com.payflow.merchant.domain.DomainEvent;

import java.util.List;

public interface DomainEventOutbox {

    void append(String aggregateId, List<DomainEvent> events);
}
