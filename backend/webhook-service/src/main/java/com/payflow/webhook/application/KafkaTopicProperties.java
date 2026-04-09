package com.payflow.webhook.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflow.kafka")
public class KafkaTopicProperties {

    private String dlqTopic = "webhook.dlq";

    public String getDlqTopic() {
        return dlqTopic;
    }

    public void setDlqTopic(String dlqTopic) {
        this.dlqTopic = dlqTopic;
    }
}
