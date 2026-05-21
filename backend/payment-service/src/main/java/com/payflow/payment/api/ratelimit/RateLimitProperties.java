package com.payflow.payment.api.ratelimit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for rate limiting under {@code payflow.rate-limit}.
 */
@ConfigurationProperties(prefix = "payflow.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private long requestsPerMinute = 100;
    private long burstCapacity = 20;
    private long cacheMaxSize = 10_000;
    private Duration cacheTtl = Duration.ofMinutes(30);
    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(long requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public long getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(long burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Map<String, EndpointRateLimit> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointRateLimit> endpoints) {
        this.endpoints = endpoints;
    }
}
