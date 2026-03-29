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
import com.payflow.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
}
