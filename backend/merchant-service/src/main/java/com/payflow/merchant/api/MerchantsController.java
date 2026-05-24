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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Merchants", description = "Merchant registration, profile management, and API key rotation")
public class MerchantsController {

    private final MerchantApplicationService merchantApplicationService;
    private final MerchantApiMapper mapper;

    public MerchantsController(MerchantApplicationService merchantApplicationService, MerchantApiMapper mapper) {
        this.merchantApplicationService = merchantApplicationService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Register a merchant", description = "Creates a new merchant account. This endpoint is public — no authentication required.")
    @SecurityRequirement(name = "")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Merchant registered — returns API key in response", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate email", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public ResponseEntity<RegisterMerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest body) {
        RegisteredMerchantResult result = merchantApplicationService.register(
                new RegisterMerchantCommand(body.getName(), body.getEmail())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toRegisterResponse(result));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current merchant profile", description = "Returns the authenticated merchant's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Merchant profile", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public MerchantResponse me() {
        Merchant merchant = merchantApplicationService.findById(MerchantContext.require());
        return mapper.toResponse(merchant);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate current merchant", description = "Deactivates the authenticated merchant's account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Merchant deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already deactivated", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deactivateMe() {
        merchantApplicationService.deactivate(MerchantContext.require());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/api-keys")
    @Operation(summary = "Rotate API key", description = "Rotates the authenticated merchant's API key and returns the new key")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New API key generated", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public RotateApiKeyResponse rotateApiKey() {
        String rawKey = merchantApplicationService.rotateApiKey(MerchantContext.require());
        return new RotateApiKeyResponse(rawKey);
    }
}
