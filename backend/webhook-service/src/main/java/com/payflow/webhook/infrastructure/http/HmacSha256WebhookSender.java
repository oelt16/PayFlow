package com.payflow.webhook.infrastructure.http;

import com.payflow.webhook.application.port.WebhookSendResult;
import com.payflow.webhook.application.port.WebhookSender;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HmacSha256WebhookSender implements WebhookSender {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestClient restClient;

    public HmacSha256WebhookSender(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public WebhookSendResult send(String url, String bodyJson, String secret, String signatureHeaderName) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] sig = mac.doFinal(bodyJson.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(sig);
            ResponseEntity<Void> response = restClient.post()
                    .uri(URI.create(url))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(signatureHeaderName, hex)
                    .body(bodyJson)
                    .retrieve()
                    .toEntity(Void.class);
            int code = response.getStatusCode().value();
            if (code >= 200 && code < 300) {
                return WebhookSendResult.ok(code);
            }
            return WebhookSendResult.fail(code, "Non-success status");
        } catch (RestClientException e) {
            return WebhookSendResult.fail(e.getMessage() != null ? e.getMessage() : "HTTP request failed");
        } catch (Exception e) {
            return WebhookSendResult.fail(e.getMessage() != null ? e.getMessage() : "signing failed");
        }
    }
}
