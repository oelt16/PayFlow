package com.payflow.payment.api;

import com.payflow.payment.api.security.MerchantContext;
import com.payflow.payment.application.CreatePaymentCommand;
import com.payflow.payment.application.CreatedPaymentResult;
import com.payflow.payment.application.PaymentApplicationService;
import com.payflow.payment.domain.CardBrand;
import com.payflow.payment.domain.CardDetails;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;
import com.payflow.payment.domain.Refund;
import com.payflow.payment.domain.RefundId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PaymentsControllerWebMvcTest {

    private static final MerchantId MERCHANT = MerchantId.of("mer_test_dev");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentApplicationService paymentApplicationService;

    @BeforeEach
    void merchantContext() {
        MerchantContext.set(MERCHANT);
    }

    @AfterEach
    void clearMerchant() {
        MerchantContext.clear();
    }

    @Test
    void postPaymentReturns201() throws Exception {
        Instant t0 = Instant.parse("2025-01-01T12:00:00Z");
        Payment payment = Payment.create(
                MERCHANT,
                Money.of(new BigDecimal("100.00"), "USD"),
                "Order #1042",
                new CardDetails("4242", CardBrand.VISA, 12, 2027),
                Map.of("orderId", "ORD-789"),
                t0
        );
        payment.pullDomainEvents();
        when(paymentApplicationService.create(eq(MERCHANT), any(CreatePaymentCommand.class)))
                .thenReturn(new CreatedPaymentResult(payment, "cs_test_xyz"));

        mockMvc.perform(
                        post("/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                {
                                                  "amount": 10000,
                                                  "currency": "USD",
                                                  "description": "Order #1042",
                                                  "card": {
                                                    "number": "4242424242424242",
                                                    "expMonth": 12,
                                                    "expYear": 2027,
                                                    "cvc": "123"
                                                  },
                                                  "metadata": { "orderId": "ORD-789" }
                                                }
                                                """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.clientSecret").value("cs_test_xyz"));
    }

    @Test
    void postRefundReturns201() throws Exception {
        String payId = "pay_1";
        Refund refund = new Refund(
                RefundId.of("re_xyz"),
                PaymentId.of(payId),
                Money.of(new BigDecimal("25.00"), "USD"),
                Optional.of("customer request"),
                Instant.parse("2025-01-02T10:00:00Z")
        );
        when(paymentApplicationService.refund(eq(MERCHANT), eq(PaymentId.of(payId)), anyLong(), anyString(), any()))
                .thenReturn(refund);
        when(paymentApplicationService.listRefunds(eq(MERCHANT), eq(PaymentId.of(payId))))
                .thenReturn(List.of(refund));

        mockMvc.perform(
                        post("/v1/payments/{id}/refunds", payId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                                { "amount": 2500, "currency": "USD", "reason": "customer request" }
                                                """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("re_xyz"))
                .andExpect(jsonPath("$.amount").value(2500));

        mockMvc.perform(get("/v1/payments/{id}/refunds", payId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].id").value("re_xyz"));
    }
}
