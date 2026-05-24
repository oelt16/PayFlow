package com.payflow.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardPayload {

    @Schema(description = "Card number (PAN)", example = "4242424242424242")
    @NotBlank
    private String number;

    @Schema(description = "Expiration month", example = "12", minimum = "1", maximum = "12")
    @NotNull
    @Min(1)
    @Max(12)
    private Integer expMonth;

    @Schema(description = "Expiration year", example = "2028", minimum = "2000", maximum = "9999")
    @NotNull
    @Min(2000)
    @Max(9999)
    private Integer expYear;

    @Schema(description = "Card verification code", example = "123")
    @NotBlank
    private String cvc;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(Integer expMonth) {
        this.expMonth = expMonth;
    }

    public Integer getExpYear() {
        return expYear;
    }

    public void setExpYear(Integer expYear) {
        this.expYear = expYear;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }
}
