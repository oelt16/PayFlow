package com.payflow.payment.api.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Unit tests for IdempotencyFilter.
 */
@DisplayName("IdempotencyFilter")
class IdempotencyFilterTest {

    @Test
    @DisplayName("should have @Order annotation with correct value")
    void hasOrderAnnotationWithCorrectValue() {
        Order orderAnnotation = IdempotencyFilter.class.getAnnotation(Order.class);
        assertTrue(orderAnnotation != null, "Filter should have @Order annotation");
        
        // The expected value is HIGHEST_PRECEDENCE + 20 = -2147483648 + 20 = -2147483628
        int expectedOrder = Ordered.HIGHEST_PRECEDENCE + 20;
        assertEquals(expectedOrder, orderAnnotation.value(),
                "Order should be HIGHEST_PRECEDENCE + 20");
    }

    @Test
    @DisplayName("should have @Component annotation")
    void hasComponentAnnotation() {
        assertTrue(IdempotencyFilter.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    @DisplayName("should extend OncePerRequestFilter")
    void extendsOncePerRequestFilter() {
        assertTrue(org.springframework.web.filter.OncePerRequestFilter.class
                .isAssignableFrom(IdempotencyFilter.class));
    }
}