package com.payflow.payment.application.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotencyConfig {

    @Bean
    public Duration idempotencyTtl() {
        // 24 hour TTL as per spec
        return Duration.ofHours(24);
    }
}