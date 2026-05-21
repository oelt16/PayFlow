package com.payflow.merchant.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RateLimitProperties default values and configuration.
 */
@DisplayName("RateLimitProperties")
class RateLimitPropertiesTest {

    @Test
    @DisplayName("should have correct default values")
    void hasCorrectDefaultValues() {
        RateLimitProperties props = new RateLimitProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getRequestsPerMinute()).isEqualTo(100);
        assertThat(props.getBurstCapacity()).isEqualTo(20);
        assertThat(props.getCacheMaxSize()).isEqualTo(10_000);
        assertThat(props.getCacheTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(props.getEndpoints()).isEmpty();
    }

    @Test
    @DisplayName("should allow setting endpoint limits")
    void shouldAllowSettingEndpointLimits() {
        RateLimitProperties props = new RateLimitProperties();
        EndpointRateLimit apiKeyLimit = new EndpointRateLimit(
                "POST", "/v1/merchants/me/api-keys", 3, Duration.ofHours(1));

        props.getEndpoints().put("api-keys-create", apiKeyLimit);

        assertThat(props.getEndpoints()).hasSize(1);
        EndpointRateLimit resolved = props.getEndpoints().get("api-keys-create");
        assertThat(resolved.method()).isEqualTo("POST");
        assertThat(resolved.path()).isEqualTo("/v1/merchants/me/api-keys");
        assertThat(resolved.tokens()).isEqualTo(3);
        assertThat(resolved.refillDuration()).isEqualTo(Duration.ofHours(1));
    }
}
