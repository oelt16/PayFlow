package com.payflow.payment.application;

import com.payflow.payment.application.port.AcquiringPort;
import com.payflow.payment.application.port.DomainEventOutbox;
import com.payflow.payment.application.port.PaymentRepository;
import com.payflow.payment.application.money.MoneyMinorUnits;
import com.payflow.payment.application.port.RefundRepository;
import com.payflow.payment.domain.CardBrand;
import com.payflow.payment.domain.CardDetails;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.DomainEvent;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;
import com.payflow.payment.domain.Refund;
import com.payflow.payment.domain.RefundId;
import com.payflow.payment.domain.event.PaymentRefundedEvent;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2025-06-01T10:00:00Z");
    private static final MerchantId MERCHANT = MerchantId.of("mer_test_dev");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private DomainEventOutbox domainEventOutbox;

    @Mock
    private AcquiringPort acquiringPort;

    @Mock
    private ClientSecretGenerator clientSecretGenerator;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new PaymentApplicationService(
                paymentRepository,
                refundRepository,
                domainEventOutbox,
                acquiringPort,
                clientSecretGenerator,
                clock
        );
    }

    @Test
    void createPersistsPaymentAndAppendsOutboxEvents() {
        when(clientSecretGenerator.newClientSecret()).thenReturn("cs_test_secret");
        CreatePaymentCommand cmd = new CreatePaymentCommand(
                10_000,
                "usd",
                "Order",
                "4242424242424242",
                12,
                2027,
                "123",
                Map.of("k", "v")
        );

        CreatedPaymentResult result = service.create(MERCHANT, cmd);

        assertThat(result.clientSecret()).isEqualTo("cs_test_secret");
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).insert(any(Payment.class), eq("cs_test_secret"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainEventOutbox).append(eq(result.payment().id().value()), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void captureWhenAlreadyCapturedIsIdempotent() {
        PaymentId id = PaymentId.of("pay_existing");
        Payment restored = Payment.restore(
                id,
                MERCHANT,
                Money.of(new BigDecimal("100.00"), "USD"),
                "x",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of(),
                NOW,
                NOW.plusSeconds(3600),
                PaymentStatus.CAPTURED,
                NOW.minusSeconds(60),
                null,
                BigDecimal.ZERO.setScale(2)
        );
        when(paymentRepository.findByIdAndMerchantId(id, MERCHANT)).thenReturn(Optional.of(restored));

        Payment after = service.capture(MERCHANT, id);

        assertThat(after.status()).isEqualTo(PaymentStatus.CAPTURED);
        verify(acquiringPort, never()).confirmCapture(any(), any(), any());
        verify(paymentRepository, never()).update(any());
        verify(domainEventOutbox, never()).append(any(), any());
    }

    @Test
    void refundPersistsRefundUpdatesPaymentAndAppendsOutbox() {
        PaymentId id = PaymentId.of("pay_cap");
        Payment captured = Payment.restore(
                id,
                MERCHANT,
                Money.of(new BigDecimal("100.00"), "USD"),
                "x",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of(),
                NOW,
                NOW.plusSeconds(3600),
                PaymentStatus.CAPTURED,
                NOW.minusSeconds(60),
                null,
                BigDecimal.ZERO.setScale(2)
        );
        when(paymentRepository.findByIdAndMerchantId(id, MERCHANT)).thenReturn(Optional.of(captured));

        Refund result = service.refund(MERCHANT, id, 2_500, "USD", Optional.of("requested"));

        assertThat(result.id().value()).startsWith("re_");
        assertThat(result.paymentId()).isEqualTo(id);
        assertThat(MoneyMinorUnits.toMinorUnits(result.amount())).isEqualTo(2_500);
        assertThat(result.reason()).contains("requested");
        verify(acquiringPort).confirmRefund(eq(id), any(), eq(MERCHANT));
        verify(refundRepository).insert(any(Refund.class));
        verify(paymentRepository).update(any(Payment.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainEventOutbox).append(eq(id.value()), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst()).isInstanceOf(PaymentRefundedEvent.class);
    }

    @Test
    void listRefundsReturnsRepositoryRows() {
        PaymentId id = PaymentId.of("pay_cap");
        Payment captured = Payment.restore(
                id,
                MERCHANT,
                Money.of(new BigDecimal("100.00"), "USD"),
                "x",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of(),
                NOW,
                NOW.plusSeconds(3600),
                PaymentStatus.CAPTURED,
                NOW.minusSeconds(60),
                null,
                BigDecimal.ZERO.setScale(2)
        );
        when(paymentRepository.findByIdAndMerchantId(id, MERCHANT)).thenReturn(Optional.of(captured));
        Refund row = new Refund(
                RefundId.of("re_1"),
                id,
                Money.of(new BigDecimal("5.00"), "USD"),
                Optional.empty(),
                NOW
        );
        when(refundRepository.findByPaymentId(id, "USD")).thenReturn(List.of(row));

        List<Refund> list = service.listRefunds(MERCHANT, id);

        assertThat(list).containsExactly(row);
    }
}
