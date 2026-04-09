package com.payflow.merchant.infrastructure.outbox;

import com.payflow.merchant.domain.DomainEvent;
import com.payflow.merchant.domain.event.MerchantCreatedEvent;
import com.payflow.merchant.domain.event.MerchantDeactivatedEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MerchantEventPayloadMapper {

    public String eventType(DomainEvent event) {
        if (event instanceof MerchantCreatedEvent) {
            return "merchant.created";
        }
        if (event instanceof MerchantDeactivatedEvent) {
            return "merchant.deactivated";
        }
        throw new IllegalArgumentException("Unsupported domain event: " + event.getClass().getName());
    }

    public Map<String, Object> payload(DomainEvent event) {
        if (event instanceof MerchantCreatedEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            String mid = e.merchantId().value();
            m.put("merchantId", mid);
            m.put("name", e.name());
            m.put("email", e.email());
            m.put("createdAt", e.createdAt().toString());
            return m;
        }
        if (event instanceof MerchantDeactivatedEvent e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("merchantId", e.merchantId().value());
            m.put("deactivatedAt", e.deactivatedAt().toString());
            return m;
        }
        throw new IllegalArgumentException("Unsupported domain event: " + event.getClass().getName());
    }
}
