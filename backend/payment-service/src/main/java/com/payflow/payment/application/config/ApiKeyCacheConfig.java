package com.payflow.payment.application.config;

import com.payflow.payment.cache.ApiKeyCache;
import com.payflow.payment.cache.CaffeineApiKeyCache;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for API key cache.
 */
@Configuration
@EnableConfigurationProperties(ApiKeyCacheProperties.class)
public class ApiKeyCacheConfig {

    @Bean
    public ApiKeyCache apiKeyCache(ApiKeyCacheProperties properties) {
        return new CaffeineApiKeyCache(
                properties.getTtlSeconds(),
                properties.getMaxSize()
        );
    }
}