package com.payflow.payment.api.ratelimit;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal test controller for rate-limit integration tests.
 * Only loaded under the ratelimit-test profile to avoid conflicting
 * with PaymentController's POST /v1/payments mapping.
 */
@RestController
@RequestMapping("/v1/payments")
@Profile("ratelimit-test")
class RateLimitTestController {

    @GetMapping("/test")
    ResponseEntity<Map<String, String>> getTest() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> postPayment(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "status", "PENDING",
                "id", "pay_test_" + System.nanoTime()
        ));
    }
}
