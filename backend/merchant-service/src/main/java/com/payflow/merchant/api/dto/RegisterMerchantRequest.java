package com.payflow.merchant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterMerchantRequest {

    @Schema(description = "Merchant business name", example = "Acme Corp")
    @NotBlank
    @Size(max = 255)
    private String name;

    @Schema(description = "Merchant contact email", example = "admin@acme.com")
    @NotBlank
    @Email
    @Size(max = 320)
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
