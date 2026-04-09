package com.payflow.merchant.domain;

import com.payflow.merchant.domain.event.MerchantCreatedEvent;
import com.payflow.merchant.domain.event.MerchantDeactivatedEvent;
import com.payflow.merchant.domain.exception.MerchantAlreadyDeactivatedException;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantTest {

    private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void registerCreatesActiveMerchantAndEmitsCreatedEvent() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );

        assertThat(m.name()).isEqualTo("Acme");
        assertThat(m.email()).isEqualTo("a@acme.com");
        assertThat(m.keyPrefix()).isEqualTo("sk_test_");
        assertThat(m.isActive()).isTrue();
        assertThat(m.createdAt()).isEqualTo(NOW);
        assertThat(m.deactivatedAt()).isNull();

        assertThat(m.pullDomainEvents()).singleElement()
                .isInstanceOf(MerchantCreatedEvent.class)
                .satisfies(e -> {
                    MerchantCreatedEvent ev = (MerchantCreatedEvent) e;
                    assertThat(ev.merchantId()).isEqualTo(m.id());
                    assertThat(ev.name()).isEqualTo("Acme");
                    assertThat(ev.email()).isEqualTo("a@acme.com");
                    assertThat(ev.createdAt()).isEqualTo(NOW);
                });
    }

    @Test
    void deactivateFromActiveEmitsEvent() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        m.pullDomainEvents();

        m.deactivate(NOW.plusSeconds(1));

        assertThat(m.isActive()).isFalse();
        assertThat(m.deactivatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(m.pullDomainEvents()).singleElement()
                .isInstanceOf(MerchantDeactivatedEvent.class);
    }

    @Test
    void deactivateWhenAlreadyInactiveThrows() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        m.pullDomainEvents();
        m.deactivate(NOW.plusSeconds(1));
        m.pullDomainEvents();

        assertThatThrownBy(() -> m.deactivate(NOW.plusSeconds(2)))
                .isInstanceOf(MerchantAlreadyDeactivatedException.class);
    }

    @Test
    void rotateApiKeyUpdatesPrefixAndHash() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$oldoldoldoldoldoldoldoldoldoldoldoldoldoldoldoldo"),
                NOW
        );
        m.pullDomainEvents();

        m.rotateApiKey("sk_test2", ApiKeyHash.of("$2a$10$newnewnewnewnewnewnewnewnewnewnewnewnewnewnewnewn"));

        assertThat(m.keyPrefix()).isEqualTo("sk_test2");
        assertThat(m.keyHash().value()).contains("newnew");
    }

    @Test
    void rotateApiKeyWhenDeactivatedThrows() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        m.pullDomainEvents();
        m.deactivate(NOW.plusSeconds(1));
        m.pullDomainEvents();

        assertThatThrownBy(() -> m.rotateApiKey("sk_x", ApiKeyHash.of("$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")))
                .isInstanceOf(MerchantAlreadyDeactivatedException.class);
    }

    @Test
    void peekDomainEventsDoesNotClearBuffer() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        assertThat(m.peekDomainEvents()).hasSize(1);
        assertThat(m.peekDomainEvents()).hasSize(1);
        assertThat(m.pullDomainEvents()).hasSize(1);
    }

    @Test
    void pullDomainEventsClearsBuffer() {
        Merchant m = Merchant.register(
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        assertThat(m.pullDomainEvents()).hasSize(1);
        assertThat(m.pullDomainEvents()).isEmpty();
    }

    @Test
    void registerRejectsBlankName() {
        assertThatThrownBy(() -> Merchant.register(
                "  ",
                "a@b.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
    }

    @Test
    void registerRejectsBlankEmail() {
        assertThatThrownBy(() -> Merchant.register(
                "Acme",
                "  ",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
    }

    @Test
    void registerRejectsBlankKeyPrefix() {
        assertThatThrownBy(() -> Merchant.register(
                "Acme",
                "a@b.com",
                "   ",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("keyPrefix");
    }

    @Test
    void rotateApiKeyRejectsBlankPrefix() {
        Merchant m = Merchant.register(
                "Acme",
                "a@b.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                NOW
        );
        m.pullDomainEvents();
        assertThatThrownBy(() -> m.rotateApiKey("  ", ApiKeyHash.of("$2a$10$newnewnewnewnewnewnewnewnewnewnewnewnewnewnewnewn")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoreDoesNotEmitEvents() {
        MerchantId id = MerchantId.of("mer_fixed");
        Merchant m = Merchant.restore(
                id,
                "Acme",
                "a@acme.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashha"),
                true,
                NOW,
                null
        );
        assertThat(m.id()).isEqualTo(id);
        assertThat(m.pullDomainEvents()).isEmpty();
    }
}
