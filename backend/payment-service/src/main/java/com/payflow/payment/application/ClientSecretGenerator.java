package com.payflow.payment.application;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class ClientSecretGenerator {

    private final SecureRandom random = new SecureRandom();

    public String newClientSecret() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        return "cs_test_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
