package com.payflow.merchant.domain;

import java.util.Objects;

/**
 * Wraps a BCrypt-hashed API key string persisted by the infrastructure layer.
 */
public final class ApiKeyHash {

    private final String value;

    private ApiKeyHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("apiKeyHash must not be blank");
        }
        this.value = value;
    }

    public static ApiKeyHash of(String bcryptHash) {
        return new ApiKeyHash(bcryptHash);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiKeyHash that = (ApiKeyHash) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
