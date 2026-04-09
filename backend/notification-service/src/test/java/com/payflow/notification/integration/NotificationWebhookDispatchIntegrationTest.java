package com.payflow.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.event.PaymentEventEnvelope;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class NotificationWebhookDispatchIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka");

    private static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
            wireMockServer.start();
        }
        registry.add(
                "payflow.webhook-dispatch.base-url",
                () -> "http://localhost:" + wireMockServer.port()
        );
        registry.add("payflow.webhook-dispatch.enabled", () -> "true");
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void kafkaEventTriggersWebhookDispatchHttpCall() throws Exception {
        wireMockServer.stubFor(
                post(urlEqualTo("/internal/webhooks/dispatch"))
                        .willReturn(aResponse().withStatus(204))
        );

        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                "evt_wm",
                "payment.created",
                "pay_wm",
                "mer_wm",
                Instant.parse("2024-09-01T12:00:00Z"),
                Map.of("status", "PENDING")
        );
        String json = objectMapper.writeValueAsString(envelope);
        kafkaTemplate.send("payments.events", "mer_wm", json).get(30, TimeUnit.SECONDS);

        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                wireMockServer.verify(
                        postRequestedFor(urlEqualTo("/internal/webhooks/dispatch"))
                                .withHeader("Content-Type", containing("application/json"))
                );
                return;
            } catch (VerificationException e) {
                Thread.sleep(200);
            }
        }
        wireMockServer.verify(
                postRequestedFor(urlEqualTo("/internal/webhooks/dispatch"))
                        .withHeader("Content-Type", containing("application/json"))
        );
    }
}
