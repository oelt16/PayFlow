package com.payflow.payment.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Unit tests for RateLimitFilter.
 */
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    @Test
    @DisplayName("should have @Order annotation with correct value")
    void hasOrderAnnotationWithCorrectValue() {
        Order orderAnnotation = RateLimitFilter.class.getAnnotation(Order.class);
        assertTrue(orderAnnotation != null, "Filter should have @Order annotation");

        // The expected value is HIGHEST_PRECEDENCE + 15 = -2147483648 + 15 = -2147483633
        int expectedOrder = Ordered.HIGHEST_PRECEDENCE + 15;
        assertEquals(expectedOrder, orderAnnotation.value(),
                "Order should be HIGHEST_PRECEDENCE + 15");
    }

    @Test
    @DisplayName("should extend OncePerRequestFilter")
    void extendsOncePerRequestFilter() {
        assertTrue(org.springframework.web.filter.OncePerRequestFilter.class
                .isAssignableFrom(RateLimitFilter.class));
    }
}
