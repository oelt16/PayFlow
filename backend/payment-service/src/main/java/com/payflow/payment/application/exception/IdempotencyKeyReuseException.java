package com.payflow.payment.application.exception;

/**
 * Exception thrown when an idempotency key is reused with a different request body.
 * This indicates a potential client error where the same idempotency key was used
 * with different request content.
 */
public final class IdempotencyKeyReuseException extends RuntimeException {

    private final String key;
    private final String storedHash;
    private final String newHash;

    public IdempotencyKeyReuseException(String key, String storedHash, String newHash) {
        super(String.format(
                "Idempotency key '%s' already used with different request body. " +
                        "Stored hash: '%s', new hash: '%s'",
                key, storedHash, newHash
        ));
        this.key = key;
        this.storedHash = storedHash;
        this.newHash = newHash;
    }

    public String getKey() {
        return key;
    }

    public String getStoredHash() {
        return storedHash;
    }

    public String getNewHash() {
        return newHash;
    }
}