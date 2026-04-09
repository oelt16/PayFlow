package com.payflow.notification.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflow.webhook-dispatch")
public class WebhookDispatchProperties {

    private boolean enabled = true;

    /**
     * Base URL of webhook-service (no trailing slash), e.g. http://webhook-service:8083
     */
    private String baseUrl = "http://localhost:8083";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
