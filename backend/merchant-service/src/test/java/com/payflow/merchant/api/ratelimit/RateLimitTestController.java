package com.payflow.merchant.api.ratelimit;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal test controller for rate-limit integration tests.
 */
@RestController
@RequestMapping("/v1/merchants")
class RateLimitTestController {

    @GetMapping("/test")
    ResponseEntity<Map<String, String>> getTest() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/me/api-keys")
    ResponseEntity<Map<String, Object>> postApiKey(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "id", "key_test_" + System.nanoTime()
        ));
    }
}
