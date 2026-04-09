package com.payflow.merchant.api;

import com.payflow.merchant.api.dto.MerchantResponse;
import com.payflow.merchant.api.dto.RegisterMerchantResponse;
import com.payflow.merchant.application.RegisteredMerchantResult;
import com.payflow.merchant.domain.Merchant;

import org.springframework.stereotype.Component;

@Component
public class MerchantApiMapper {

    public RegisterMerchantResponse toRegisterResponse(RegisteredMerchantResult result) {
        Merchant m = result.merchant();
        return new RegisterMerchantResponse(
                m.id().value(),
                m.name(),
                m.email(),
                result.rawApiKey(),
                m.createdAt()
        );
    }

    public MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.id().value(),
                merchant.name(),
                merchant.email(),
                merchant.isActive(),
                merchant.createdAt()
        );
    }
}
