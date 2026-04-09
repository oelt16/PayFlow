package com.payflow.merchant.application;

import com.payflow.merchant.domain.Merchant;

public record RegisteredMerchantResult(Merchant merchant, String rawApiKey) {
}
