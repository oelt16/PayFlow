package com.payflow.webhook.api.security;

import com.payflow.webhook.domain.MerchantId;

public final class MerchantContext {

    private static final ThreadLocal<MerchantId> CURRENT = new ThreadLocal<>();

    private MerchantContext() {
    }

    public static void set(MerchantId merchantId) {
        CURRENT.set(merchantId);
    }

    public static MerchantId require() {
        MerchantId id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No merchant in context");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
