package com.payflow.merchant.api.internal;

import com.payflow.merchant.api.dto.ErrorResponse;
import com.payflow.merchant.api.dto.ValidateKeyRequest;
import com.payflow.merchant.api.dto.ValidateKeyResponse;
import com.payflow.merchant.application.port.MerchantRepository;
import com.payflow.merchant.domain.Merchant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal controller for validating API key prefixes.
 * Used by payment-service and webhook-service for cache validation.
 */
@RestController
@RequestMapping("/v1/internal/merchants")
public class ValidateKeyController {

    private final MerchantRepository merchantRepository;

    public ValidateKeyController(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @PostMapping("/validate-key")
    public ResponseEntity<?> validateKey(@RequestBody ValidateKeyRequest request) {
        if (request == null || request.keyPrefix() == null || request.keyPrefix().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("key_prefix_required"));
        }

        List<Merchant> merchants = merchantRepository.findByKeyPrefix(request.keyPrefix());

        // Find the first active merchant with this key prefix
        for (Merchant merchant : merchants) {
            if (merchant.isActive()) {
                return ResponseEntity.ok(new ValidateKeyResponse(
                        merchant.id().value(),
                        merchant.keyHash().value(),
                        merchant.isActive()
                ));
            }
        }

        // If we found merchants but none are active, return inactive response
        if (!merchants.isEmpty()) {
            Merchant inactive = merchants.get(0);
            return ResponseEntity.ok(new ValidateKeyResponse(
                    inactive.id().value(),
                    inactive.keyHash().value(),
                    false
            ));
        }

        // No merchant found with this key prefix
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("key_not_found"));
    }
}