package com.payflow.merchant.application;

import com.payflow.merchant.application.exception.InvalidApiKeyException;
import com.payflow.merchant.application.exception.MerchantNotFoundException;
import com.payflow.merchant.application.port.ApiKeyHasher;
import com.payflow.merchant.application.port.DomainEventOutbox;
import com.payflow.merchant.application.port.MerchantRepository;
import com.payflow.merchant.domain.ApiKeyHash;
import com.payflow.merchant.domain.Merchant;
import com.payflow.merchant.domain.MerchantId;
import com.payflow.merchant.domain.exception.DuplicateEmailException;
import com.payflow.merchant.domain.exception.MerchantAlreadyDeactivatedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2025-06-01T10:00:00Z");

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private DomainEventOutbox outboxAppender;

    @Mock
    private ApiKeyHasher apiKeyHasher;

    private final ApiKeyGenerator apiKeyGenerator = new ApiKeyGenerator();

    private MerchantApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new MerchantApplicationService(
                merchantRepository,
                outboxAppender,
                apiKeyHasher,
                apiKeyGenerator,
                clock
        );
    }

    @Test
    void registerPersistsAndAppendsOutboxEvents() {
        when(merchantRepository.existsByEmail("a@acme.com")).thenReturn(false);
        when(apiKeyHasher.hash(any())).thenReturn("$2a$10$hashed");

        RegisteredMerchantResult result = service.register(new RegisterMerchantCommand("  Acme  ", "  A@Acme.COM  "));

        assertThat(result.merchant().name()).isEqualTo("Acme");
        assertThat(result.merchant().email()).isEqualTo("a@acme.com");
        assertThat(result.rawApiKey()).startsWith("sk_test_");
        verify(merchantRepository).insert(any(Merchant.class));
        verify(outboxAppender).append(any(String.class), any());
        verify(apiKeyHasher).hash(result.rawApiKey());
    }

    @Test
    void registerDuplicateEmailThrows() {
        when(merchantRepository.existsByEmail("x@y.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterMerchantCommand("N", "x@y.com")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(merchantRepository, never()).insert(any());
        verify(outboxAppender, never()).append(any(), any());
    }

    @Test
    void deactivateUpdatesAndAppendsEvent() {
        MerchantId id = MerchantId.of("mer_1");
        Merchant m = Merchant.restore(
                id,
                "A",
                "a@b.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$h"),
                true,
                NOW,
                null
        );
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));

        service.deactivate(id);

        assertThat(m.isActive()).isFalse();
        verify(merchantRepository).update(m);
        verify(outboxAppender).append(eq(id.value()), any());
    }

    @Test
    void deactivateMerchantNotFoundThrows() {
        MerchantId id = MerchantId.of("mer_missing");
        when(merchantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void rotateApiKeyReturnsNewRawKeyAndUpdates() {
        MerchantId id = MerchantId.of("mer_1");
        Merchant m = Merchant.restore(
                id,
                "A",
                "a@b.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$old"),
                true,
                NOW,
                null
        );
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));
        when(apiKeyHasher.hash(any())).thenReturn("$2a$10$new");

        String newKey = service.rotateApiKey(id);

        assertThat(newKey).startsWith("sk_test_");
        assertThat(m.keyPrefix()).isEqualTo(ApiKeyGenerator.keyPrefix(newKey));
        verify(merchantRepository).update(m);
    }

    @Test
    void authenticateMatchesByPrefixAndBcrypt() {
        String raw = "sk_test_" + "a".repeat(32);
        String prefix = ApiKeyGenerator.keyPrefix(raw);
        Merchant m = Merchant.register("A", "a@b.com", prefix, ApiKeyHash.of("$2a$10$stored"), NOW);
        m.pullDomainEvents();
        when(merchantRepository.findByKeyPrefix(prefix)).thenReturn(List.of(m));
        when(apiKeyHasher.matches(raw, "$2a$10$stored")).thenReturn(true);

        Merchant auth = service.authenticate(raw);

        assertThat(auth.id()).isEqualTo(m.id());
    }

    @Test
    void authenticateSkipsInactiveMerchant() {
        String raw = "sk_test_" + "b".repeat(32);
        String prefix = ApiKeyGenerator.keyPrefix(raw);
        Merchant m = Merchant.register("A", "a@b.com", prefix, ApiKeyHash.of("$2a$10$stored"), NOW);
        m.pullDomainEvents();
        m.deactivate(NOW.plusSeconds(1));
        m.pullDomainEvents();
        when(merchantRepository.findByKeyPrefix(prefix)).thenReturn(List.of(m));

        assertThatThrownBy(() -> service.authenticate(raw))
                .isInstanceOf(InvalidApiKeyException.class);
    }

    @Test
    void deactivateWhenAlreadyInactiveThrows() {
        MerchantId id = MerchantId.of("mer_1");
        Merchant m = Merchant.restore(
                id,
                "A",
                "a@b.com",
                "sk_test_",
                ApiKeyHash.of("$2a$10$h"),
                false,
                NOW,
                NOW.plusSeconds(5)
        );
        when(merchantRepository.findById(id)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.deactivate(id))
                .isInstanceOf(MerchantAlreadyDeactivatedException.class);
        verify(merchantRepository, never()).update(any());
        verify(outboxAppender, never()).append(any(), any());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        MerchantId id = MerchantId.of("mer_x");
        when(merchantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
