package com.payflow.webhook.api.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflow.security")
public class PayflowSecurityProperties {

    private List<ApiKeyEntry> apiKeys = new ArrayList<>();

    public List<ApiKeyEntry> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<ApiKeyEntry> apiKeys) {
        this.apiKeys = apiKeys != null ? apiKeys : new ArrayList<>();
    }

    public static class ApiKeyEntry {

        private String key;
        private String merchantId;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }
    }
}
