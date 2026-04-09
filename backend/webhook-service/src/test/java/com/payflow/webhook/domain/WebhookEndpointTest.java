package com.payflow.webhook.domain;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookEndpointTest {

    private static final Instant T0 = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void httpUrlRejected() {
        assertThatThrownBy(() -> WebhookEndpoint.register(
                MerchantId.of("mer_1"),
                "http://example.com/hook",
                Set.of("payment.created"),
                T0
        )).isInstanceOf(InvalidWebhookUrlException.class);
    }

    @Test
    void registerCreatesHttpsEndpoint() {
        WebhookEndpoint e = WebhookEndpoint.register(
                MerchantId.of("mer_1"),
                " https://example.com/hook ",
                Set.of("payment.created", "payment.captured"),
                T0
        );
        assertThat(e.url()).isEqualTo("https://example.com/hook");
        assertThat(e.active()).isTrue();
        assertThat(e.secret()).isNotBlank();
        assertThat(e.matchesEvent("payment.created")).isTrue();
        assertThat(e.matchesEvent("payment.refunded")).isFalse();
    }

    @Test
    void deactivateStopsMatching() {
        WebhookEndpoint e = WebhookEndpoint.register(
                MerchantId.of("mer_1"),
                "https://example.com/hook",
                Set.of("payment.created"),
                T0
        );
        e.deactivate();
        assertThat(e.active()).isFalse();
        assertThat(e.matchesEvent("payment.created")).isFalse();
    }
}
