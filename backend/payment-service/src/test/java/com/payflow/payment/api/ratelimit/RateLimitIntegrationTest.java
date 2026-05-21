package com.payflow.payment.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payment.api.security.MerchantContext;
import com.payflow.payment.domain.MerchantId;

/**
 * Integration tests for rate limiting behavior using standalone MockMvc.
 * Does not require database or Kafka — tests the filter in isolation.
 */
@DisplayName("RateLimit Integration")
class RateLimitIntegrationTest {

    private MockMvc mockMvc;
    private RateLimitProperties properties;
    private BucketRegistry registry;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setBurstCapacity(5);
        properties.setCacheMaxSize(10_000);
        properties.setCacheTtl(Duration.ofMinutes(30));
        properties.getEndpoints().put("payments-create",
                new EndpointRateLimit("POST", "/v1/payments", 3, Duration.ofMinutes(1)));

        registry = new BucketRegistry(properties);
        filter = new RateLimitFilter(registry, properties, new ObjectMapper());

        mockMvc = MockMvcBuilders.standaloneSetup(new RateLimitTestController())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        MerchantContext.clear();
    }

    @Test
    @DisplayName("should return 429 after exhausting token bucket")
    void shouldReturn429AfterExhaustingTokens() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        // Send 5 requests that should succeed (burst capacity = 5)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/payments/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 6th request should be rate limited
        mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.requestId").exists());
    }

    @Test
    @DisplayName("should include rate limit headers on successful response")
    void shouldIncludeRateLimitHeadersOnSuccess() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        MvcResult result = mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andReturn();

        String remaining = result.getResponse().getHeader("X-RateLimit-Remaining");
        assertThat(remaining).isNotNull();
        assertThat(Integer.parseInt(remaining)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("should include all four headers on 429 response")
    void shouldIncludeAllHeadersOn429() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        // Exhaust the bucket (5 tokens)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/payments/test")
                    .contentType(MediaType.APPLICATION_JSON));
        }

        // 6th request should be 429 with all headers
        MvcResult result = mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        String retryAfter = result.getResponse().getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isGreaterThan(0);
    }

    @Test
    @DisplayName("should enforce stricter limit on POST /v1/payments")
    void shouldEnforceStricterPaymentLimit() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        String body = "{\"amount\": 1000, \"currency\": \"USD\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // 4th POST should be rate limited
        mockMvc.perform(post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("rate_limit_exceeded"));

        // GET should still work (different bucket)
        mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should skip rate limiting when merchant context is not set")
    void shouldSkipWhenNoMerchantContext() throws Exception {
        // Don't set MerchantContext
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/v1/payments/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("should skip rate limiting for non /v1/ paths")
    void shouldSkipNonV1Paths() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        // This path doesn't start with /v1/ so should pass through
        // (the test controller doesn't handle it, so it returns 404, not 429)
        mockMvc.perform(get("/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should use separate buckets for different merchants")
    void shouldUseSeparateBucketsForDifferentMerchants() throws Exception {
        // Merchant 1 exhausts its bucket
        MerchantContext.set(MerchantId.of("mer_1"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/payments/test")
                    .contentType(MediaType.APPLICATION_JSON));
        }
        // Merchant 1 should be rate limited
        mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());

        // Merchant 2 should still have tokens
        MerchantContext.set(MerchantId.of("mer_2"));
        mockMvc.perform(get("/v1/payments/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
