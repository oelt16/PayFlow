package com.payflow.merchant.api;

import com.payflow.merchant.api.dto.MerchantResponse;
import com.payflow.merchant.api.dto.RegisterMerchantRequest;
import com.payflow.merchant.api.dto.RegisterMerchantResponse;
import com.payflow.merchant.api.dto.RotateApiKeyResponse;
import com.payflow.merchant.api.security.MerchantContext;
import com.payflow.merchant.application.MerchantApplicationService;
import com.payflow.merchant.application.RegisterMerchantCommand;
import com.payflow.merchant.application.RegisteredMerchantResult;
import com.payflow.merchant.domain.Merchant;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/merchants")
public class MerchantsController {

    private final MerchantApplicationService merchantApplicationService;
    private final MerchantApiMapper mapper;

    public MerchantsController(MerchantApplicationService merchantApplicationService, MerchantApiMapper mapper) {
        this.merchantApplicationService = merchantApplicationService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RegisterMerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest body) {
        RegisteredMerchantResult result = merchantApplicationService.register(
                new RegisterMerchantCommand(body.getName(), body.getEmail())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegisterResponse(result));
    }

    @GetMapping("/me")
    public MerchantResponse me() {
        Merchant merchant = merchantApplicationService.findById(MerchantContext.require());
        return mapper.toResponse(merchant);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMe() {
        merchantApplicationService.deactivate(MerchantContext.require());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/api-keys")
    public RotateApiKeyResponse rotateApiKey() {
        String rawKey = merchantApplicationService.rotateApiKey(MerchantContext.require());
        return new RotateApiKeyResponse(rawKey);
    }
}
