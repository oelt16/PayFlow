package com.payflow.merchant.domain;

import com.payflow.merchant.domain.event.MerchantCreatedEvent;
import com.payflow.merchant.domain.event.MerchantDeactivatedEvent;
import com.payflow.merchant.domain.exception.MerchantAlreadyDeactivatedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Merchant {

    private final MerchantId id;
    private final String name;
    private final String email;
    private final Instant createdAt;

    private String keyPrefix;
    private ApiKeyHash keyHash;
    private boolean active;
    private Instant deactivatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Merchant(
            MerchantId id,
            String name,
            String email,
            String keyPrefix,
            ApiKeyHash keyHash,
            boolean active,
            Instant createdAt,
            Instant deactivatedAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.active = active;
        this.createdAt = createdAt;
        this.deactivatedAt = deactivatedAt;
    }

    public static Merchant register(
            String name,
            String email,
            String keyPrefix,
            ApiKeyHash keyHash,
            Instant createdAt
    ) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(keyPrefix, "keyPrefix");
        Objects.requireNonNull(keyHash, "keyHash");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (keyPrefix.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        MerchantId id = MerchantId.generate();
        Merchant merchant = new Merchant(id, name, email, keyPrefix, keyHash, true, createdAt, null);
        merchant.recordEvent(new MerchantCreatedEvent(createdAt, id, name, email, createdAt));
        return merchant;
    }

    /**
     * Rehydrates from persistence; does not record domain events.
     */
    public static Merchant restore(
            MerchantId id,
            String name,
            String email,
            String keyPrefix,
            ApiKeyHash keyHash,
            boolean active,
            Instant createdAt,
            Instant deactivatedAt
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(keyPrefix, "keyPrefix");
        Objects.requireNonNull(keyHash, "keyHash");
        Objects.requireNonNull(createdAt, "createdAt");
        return new Merchant(id, name, email, keyPrefix, keyHash, active, createdAt, deactivatedAt);
    }

    public void deactivate(Instant now) {
        Objects.requireNonNull(now, "now");
        if (!active) {
            throw new MerchantAlreadyDeactivatedException("Merchant is already deactivated");
        }
        this.active = false;
        this.deactivatedAt = now;
        recordEvent(new MerchantDeactivatedEvent(now, id, now));
    }

    public void rotateApiKey(String newKeyPrefix, ApiKeyHash newHash) {
        Objects.requireNonNull(newKeyPrefix, "newKeyPrefix");
        Objects.requireNonNull(newHash, "newHash");
        if (newKeyPrefix.isBlank()) {
            throw new IllegalArgumentException("newKeyPrefix must not be blank");
        }
        if (!active) {
            throw new MerchantAlreadyDeactivatedException("Cannot rotate API key for deactivated merchant");
        }
        this.keyPrefix = newKeyPrefix;
        this.keyHash = newHash;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    public List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public MerchantId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String keyPrefix() {
        return keyPrefix;
    }

    public ApiKeyHash keyHash() {
        return keyHash;
    }

    public boolean isActive() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant deactivatedAt() {
        return deactivatedAt;
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }
}
