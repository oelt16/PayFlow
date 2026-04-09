package com.payflow.merchant.application.port;

public interface ApiKeyHasher {

    String hash(String rawApiKey);

    boolean matches(String rawApiKey, String bcryptHash);
}
