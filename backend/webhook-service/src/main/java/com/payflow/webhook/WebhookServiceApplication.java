package com.payflow.webhook;

import com.payflow.webhook.application.InternalDispatchProperties;
import com.payflow.webhook.application.KafkaTopicProperties;
import com.payflow.webhook.application.WebhookProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        WebhookProperties.class,
        InternalDispatchProperties.class,
        KafkaTopicProperties.class
})
public class WebhookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookServiceApplication.class, args);
    }
}
