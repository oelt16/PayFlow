package com.payflow.webhook.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflow.webhook")
public class WebhookProperties {

    private int maxEndpointsPerMerchant = 5;
    private String signatureHeader = "Payflow-Signature";

    public int getMaxEndpointsPerMerchant() {
        return maxEndpointsPerMerchant;
    }

    public void setMaxEndpointsPerMerchant(int maxEndpointsPerMerchant) {
        this.maxEndpointsPerMerchant = maxEndpointsPerMerchant;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
        this.signatureHeader = signatureHeader;
    }
}
