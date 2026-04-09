package com.payflow.webhook.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflow.internal")
public class InternalDispatchProperties {

    private boolean dispatchEnabled = true;

    public boolean isDispatchEnabled() {
        return dispatchEnabled;
    }

    public void setDispatchEnabled(boolean dispatchEnabled) {
        this.dispatchEnabled = dispatchEnabled;
    }
}
