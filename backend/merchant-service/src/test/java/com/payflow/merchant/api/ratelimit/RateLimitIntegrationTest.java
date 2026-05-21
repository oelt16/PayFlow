package com.payflow.merchant.api.ratelimit;

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
import com.payflow.merchant.api.security.MerchantContext;
import com.payflow.merchant.domain.MerchantId;

/**
 * Integration tests for rate limiting behavior using standalone MockMvc.
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
        properties.getEndpoints().put("api-keys-create",
                new EndpointRateLimit("POST", "/v1/merchants/me/api-keys", 3, Duration.ofHours(1)));

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

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/merchants/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/v1/merchants/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("rate_limit_exceeded"));
    }

    @Test
    @DisplayName("should include rate limit headers on successful response")
    void shouldIncludeRateLimitHeadersOnSuccess() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        MvcResult result = mockMvc.perform(get("/v1/merchants/test")
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

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/merchants/test")
                    .contentType(MediaType.APPLICATION_JSON));
        }

        MvcResult result = mockMvc.perform(get("/v1/merchants/test")
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
    @DisplayName("should enforce stricter limit on POST /v1/merchants/me/api-keys")
    void shouldEnforceStricterApiKeyLimit() throws Exception {
        MerchantContext.set(MerchantId.of("mer_ratelimit_test"));

        String body = "{\"name\": \"test-key\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/merchants/me/api-keys")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/v1/merchants/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("rate_limit_exceeded"));

        // GET should still work (different bucket)
        mockMvc.perform(get("/v1/merchants/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should skip rate limiting when merchant context is not set")
    void shouldSkipWhenNoMerchantContext() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/v1/merchants/test")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("should use separate buckets for different merchants")
    void shouldUseSeparateBucketsForDifferentMerchants() throws Exception {
        MerchantContext.set(MerchantId.of("mer_1"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/v1/merchants/test")
                    .contentType(MediaType.APPLICATION_JSON));
        }
        mockMvc.perform(get("/v1/merchants/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());

        MerchantContext.set(MerchantId.of("mer_2"));
        mockMvc.perform(get("/v1/merchants/test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
