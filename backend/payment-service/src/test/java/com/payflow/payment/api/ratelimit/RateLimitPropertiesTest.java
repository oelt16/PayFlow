package com.payflow.payment.api.ratelimit;

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
        EndpointRateLimit paymentLimit = new EndpointRateLimit(
                "POST", "/v1/payments", 20, Duration.ofMinutes(1));

        props.getEndpoints().put("payments-create", paymentLimit);

        assertThat(props.getEndpoints()).hasSize(1);
        EndpointRateLimit resolved = props.getEndpoints().get("payments-create");
        assertThat(resolved.method()).isEqualTo("POST");
        assertThat(resolved.path()).isEqualTo("/v1/payments");
        assertThat(resolved.tokens()).isEqualTo(20);
        assertThat(resolved.refillDuration()).isEqualTo(Duration.ofMinutes(1));
    }
}
