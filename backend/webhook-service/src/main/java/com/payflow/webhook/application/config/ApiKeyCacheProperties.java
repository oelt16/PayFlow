package com.payflow.webhook.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for API key cache.
 */
@ConfigurationProperties(prefix = "payflow.api-key-cache")
public class ApiKeyCacheProperties {

    private int ttlSeconds = 600;
    private int maxSize = 10000;
    private InternalEndpoint internalEndpoint = new InternalEndpoint();

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public InternalEndpoint getInternalEndpoint() {
        return internalEndpoint;
    }

    public void setInternalEndpoint(InternalEndpoint internalEndpoint) {
        this.internalEndpoint = internalEndpoint;
    }

    public static class InternalEndpoint {
        private String baseUrl = "http://localhost:8082";
        private String path = "/v1/internal/merchants/validate-key";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}