package com.payflow.payment.api.dto;

public record CardResponse(String last4, String brand, int expMonth, int expYear) {
}
