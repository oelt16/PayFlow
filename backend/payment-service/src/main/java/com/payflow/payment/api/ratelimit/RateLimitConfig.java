package com.payflow.payment.api.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for rate limiting components.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public BucketRegistry bucketRegistry(RateLimitProperties properties) {
        return new BucketRegistry(properties);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            BucketRegistry bucketRegistry,
            RateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(bucketRegistry, properties, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        return registration;
    }
}
