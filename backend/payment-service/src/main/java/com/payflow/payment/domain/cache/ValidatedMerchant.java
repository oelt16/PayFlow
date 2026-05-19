package com.payflow.payment.domain.cache;

import java.time.Instant;
import java.util.UUID;

/**
 * Cached merchant validation result containing key validation data.
 */
public record ValidatedMerchant(
        UUID merchantId,
        String keyHash,
        boolean isActive,
        Instant cachedAt
) {}