package com.payflow.merchant.infrastructure.security;

import com.payflow.merchant.application.port.ApiKeyHasher;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptApiKeyHasher implements ApiKeyHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawApiKey) {
        return encoder.encode(rawApiKey);
    }

    @Override
    public boolean matches(String rawApiKey, String bcryptHash) {
        return encoder.matches(rawApiKey, bcryptHash);
    }
}
