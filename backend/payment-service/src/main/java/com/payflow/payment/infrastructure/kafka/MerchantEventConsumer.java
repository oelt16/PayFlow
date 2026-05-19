package com.payflow.payment.infrastructure.kafka;

import com.payflow.payment.cache.ApiKeyCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for merchant events.
 * Evicts cached API keys when a merchant is deactivated.
 */
@Component
public class MerchantEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantEventConsumer.class);

    private final ApiKeyCache apiKeyCache;

    public MerchantEventConsumer(ApiKeyCache apiKeyCache) {
        this.apiKeyCache = apiKeyCache;
    }

    @KafkaListener(topics = "merchant.events", groupId = "payment-service")
    public void consume(String message) {
        try {
            Map<String, Object> event = parseEvent(message);
            String eventType = (String) event.get("eventType");

            if ("merchant.deactivated".equals(eventType)) {
                Map<String, Object> payload = (Map<String, Object>) event.get("payload");
                String keyPrefix = (String) payload.get("keyPrefix");

                if (keyPrefix != null) {
                    log.info("Evicting cached API key for deactivated merchant: {}", keyPrefix);
                    apiKeyCache.evict(keyPrefix);
                }
            }
        } catch (Exception e) {
            log.error("Error processing merchant event: {}", message, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEvent(String message) {
        // Simple JSON parsing - in production use Jackson
        Map<String, Object> event = new java.util.HashMap<>();
        
        // Basic parsing for the event structure
        if (message.contains("merchant.deactivated")) {
            event.put("eventType", "merchant.deactivated");
            
            // Extract keyPrefix from payload
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"keyPrefix\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            
            if (matcher.find()) {
                Map<String, Object> payload = new java.util.HashMap();
                payload.put("keyPrefix", matcher.group(1));
                event.put("payload", payload);
            }
        }
        
        return event;
    }
}