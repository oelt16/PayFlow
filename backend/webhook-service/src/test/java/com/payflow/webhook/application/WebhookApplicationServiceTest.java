package com.payflow.webhook.application;

import com.payflow.webhook.application.port.WebhookDeliveryRepository;
import com.payflow.webhook.application.port.WebhookDlqPublisher;
import com.payflow.webhook.application.port.WebhookEndpointRepository;
import com.payflow.webhook.application.port.WebhookSendResult;
import com.payflow.webhook.application.port.WebhookSender;
import com.payflow.webhook.domain.DeliveryStatus;
import com.payflow.webhook.domain.MaxWebhookEndpointsExceededException;
import com.payflow.webhook.domain.MerchantId;
import com.payflow.webhook.domain.WebhookDelivery;
import com.payflow.webhook.domain.WebhookEndpoint;
import com.payflow.webhook.domain.WebhookId;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2025-06-01T10:00:00Z");
    private static final MerchantId MERCHANT = MerchantId.of("mer_test");

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebhookSender webhookSender;

    @Mock
    private WebhookDlqPublisher dlqPublisher;

    private WebhookApplicationService service;

    @BeforeEach
    void setUp() {
        WebhookProperties props = new WebhookProperties();
        props.setMaxEndpointsPerMerchant(5);
        props.setSignatureHeader("Payflow-Signature");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new WebhookApplicationService(
                endpointRepository,
                deliveryRepository,
                webhookSender,
                dlqPublisher,
                props,
                clock
        );
    }

    @Test
    void registerThrowsWhenAtCap() {
        when(endpointRepository.countActiveByMerchantId(MERCHANT)).thenReturn(5L);
        assertThatThrownBy(() -> service.register(MERCHANT, "https://a.com/h", Set.of("payment.created")))
                .isInstanceOf(MaxWebhookEndpointsExceededException.class);
    }

    @Test
    void dispatchCreatesDeliveryAndSends() {
        WebhookEndpoint ep = WebhookEndpoint.register(MERCHANT, "https://hook.example/x", Set.of("payment.created"), NOW);
        when(endpointRepository.findActiveByMerchantIdAndEventType(MERCHANT, "payment.created"))
                .thenReturn(List.of(ep));
        when(webhookSender.send(eq(ep.url()), any(), eq(ep.secret()), eq("Payflow-Signature")))
                .thenReturn(WebhookSendResult.ok(200));

        service.dispatch(MERCHANT, "payment.created", "{\"a\":1}");

        ArgumentCaptor<WebhookDelivery> cap = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues().getLast().status()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(dlqPublisher, never()).publishFailedDelivery(any(), any());
    }
}
