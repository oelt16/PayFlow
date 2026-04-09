package com.payflow.merchant.application;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public final class ApiKeyGenerator {

    private static final String PREFIX = "sk_test_";
    private static final int RANDOM_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    public String newApiKey() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return PREFIX + HexFormat.of().formatHex(bytes);
    }

    /**
     * First 8 characters of the raw key for indexed lookup before BCrypt verification.
     */
    public static String keyPrefix(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < 8) {
            throw new IllegalArgumentException("rawApiKey must be at least 8 characters");
        }
        return rawApiKey.substring(0, 8);
    }
}
