package com.payflow.webhook.domain;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDeliveryTest {

    private static final Instant T0 = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void newDeliveryIsDueImmediately() {
        WebhookDelivery d = WebhookDelivery.createPending(
                WebhookId.of("wh_1"),
                "payment.created",
                "{\"k\":1}",
                T0
        );
        assertThat(d.isDue(T0)).isTrue();
        assertThat(d.attempts()).isZero();
    }

    @Test
    void successMarksDelivered() {
        WebhookDelivery d = WebhookDelivery.createPending(
                WebhookId.of("wh_1"),
                "payment.created",
                "{}",
                T0
        );
        d.recordSuccess(T0.plusSeconds(1));
        assertThat(d.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(d.isDue(T0.plusSeconds(2))).isFalse();
    }

    @Test
    void failuresScheduleBackoffAndCapAtFive() {
        WebhookDelivery d = WebhookDelivery.createPending(
                WebhookId.of("wh_1"),
                "payment.created",
                "{}",
                T0
        );
        Instant t = T0;
        for (int i = 0; i < 4; i++) {
            d.recordFailure("err", t);
            assertThat(d.status()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(d.nextRetryAt()).isNotNull();
            t = d.nextRetryAt().plusSeconds(1);
            assertThat(d.isDue(t)).isTrue();
        }
        d.recordFailure("final", t);
        assertThat(d.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d.attempts()).isEqualTo(WebhookDelivery.MAX_ATTEMPTS);
    }

    @Test
    void backoffDurationsMatchSpec() {
        assertThat(WebhookDelivery.backoffAfterAttempt(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(WebhookDelivery.backoffAfterAttempt(2)).isEqualTo(Duration.ofSeconds(30));
        assertThat(WebhookDelivery.backoffAfterAttempt(3)).isEqualTo(Duration.ofMinutes(2));
        assertThat(WebhookDelivery.backoffAfterAttempt(4)).isEqualTo(Duration.ofMinutes(10));
        assertThat(WebhookDelivery.backoffAfterAttempt(5)).isEqualTo(Duration.ofHours(1));
    }
}
