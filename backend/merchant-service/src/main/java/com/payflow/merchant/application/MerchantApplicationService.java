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

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantApplicationService {

    private final MerchantRepository merchantRepository;
    private final DomainEventOutbox outboxAppender;
    private final ApiKeyHasher apiKeyHasher;
    private final ApiKeyGenerator apiKeyGenerator;
    private final Clock clock;

    public MerchantApplicationService(
            MerchantRepository merchantRepository,
            DomainEventOutbox outboxAppender,
            ApiKeyHasher apiKeyHasher,
            ApiKeyGenerator apiKeyGenerator,
            Clock clock
    ) {
        this.merchantRepository = merchantRepository;
        this.outboxAppender = outboxAppender;
        this.apiKeyHasher = apiKeyHasher;
        this.apiKeyGenerator = apiKeyGenerator;
        this.clock = clock;
    }

    @Transactional
    public RegisteredMerchantResult register(RegisterMerchantCommand command) {
        String email = command.email().trim().toLowerCase();
        if (merchantRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        String rawKey = apiKeyGenerator.newApiKey();
        String prefix = ApiKeyGenerator.keyPrefix(rawKey);
        String hash = apiKeyHasher.hash(rawKey);
        Merchant merchant = Merchant.register(
                command.name().trim(),
                email,
                prefix,
                ApiKeyHash.of(hash),
                clock.instant()
        );
        merchantRepository.insert(merchant);
        outboxAppender.append(merchant.id().value(), merchant.pullDomainEvents());
        return new RegisteredMerchantResult(merchant, rawKey);
    }

    @Transactional(readOnly = true)
    public Merchant findById(MerchantId id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("No merchant found with id: " + id.value()));
    }

    @Transactional
    public void deactivate(MerchantId id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("No merchant found with id: " + id.value()));
        merchant.deactivate(clock.instant());
        merchantRepository.update(merchant);
        outboxAppender.append(merchant.id().value(), merchant.pullDomainEvents());
    }

    @Transactional
    public String rotateApiKey(MerchantId id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("No merchant found with id: " + id.value()));
        String rawKey = apiKeyGenerator.newApiKey();
        String prefix = ApiKeyGenerator.keyPrefix(rawKey);
        String hash = apiKeyHasher.hash(rawKey);
        merchant.rotateApiKey(prefix, ApiKeyHash.of(hash));
        merchantRepository.update(merchant);
        return rawKey;
    }

    /**
     * Resolves a merchant from raw API key using prefix lookup and BCrypt match.
     */
    @Transactional(readOnly = true)
    public Merchant authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.length() < 8) {
            throw new InvalidApiKeyException("Unknown API key");
        }
        String prefix = ApiKeyGenerator.keyPrefix(rawApiKey);
        List<Merchant> candidates = merchantRepository.findByKeyPrefix(prefix);
        for (Merchant m : candidates) {
            if (!m.isActive()) {
                continue;
            }
            if (apiKeyHasher.matches(rawApiKey, m.keyHash().value())) {
                return m;
            }
        }
        throw new InvalidApiKeyException("Unknown API key");
    }
}
