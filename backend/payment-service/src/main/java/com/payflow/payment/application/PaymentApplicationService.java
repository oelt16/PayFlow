package com.payflow.payment.application;

import com.payflow.payment.application.card.StubCardDetailsFactory;
import com.payflow.payment.application.exception.PaymentNotFoundException;
import com.payflow.payment.application.money.MoneyMinorUnits;
import com.payflow.payment.application.pagination.PageRequest;
import com.payflow.payment.application.pagination.PageResult;
import com.payflow.payment.application.port.AcquiringPort;
import com.payflow.payment.application.port.DomainEventOutbox;
import com.payflow.payment.application.port.PaymentRepository;
import com.payflow.payment.domain.CardDetails;
import com.payflow.payment.domain.MerchantId;
import com.payflow.payment.domain.Money;
import com.payflow.payment.domain.Payment;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final DomainEventOutbox outboxAppender;
    private final AcquiringPort acquiringPort;
    private final ClientSecretGenerator clientSecretGenerator;
    private final Clock clock;

    public PaymentApplicationService(
            PaymentRepository paymentRepository,
            DomainEventOutbox outboxAppender,
            AcquiringPort acquiringPort,
            ClientSecretGenerator clientSecretGenerator,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.outboxAppender = outboxAppender;
        this.acquiringPort = acquiringPort;
        this.clientSecretGenerator = clientSecretGenerator;
        this.clock = clock;
    }

    @Transactional
    public CreatedPaymentResult create(MerchantId merchantId, CreatePaymentCommand command) {
        Money amount = MoneyMinorUnits.toMoney(command.amountMinor(), command.currency());
        CardDetails card = StubCardDetailsFactory.from(command.cardNumber(), command.expMonth(), command.expYear());
        Map<String, String> metadata = command.metadata() != null ? command.metadata() : Map.of();
        String description = command.description() != null ? command.description() : "";
        Instant now = clock.instant();
        Payment payment = Payment.create(merchantId, amount, description, card, metadata, now);
        String clientSecret = clientSecretGenerator.newClientSecret();
        paymentRepository.insert(payment, clientSecret);
        outboxAppender.append(payment.id().value(), payment.pullDomainEvents());
        return new CreatedPaymentResult(payment, clientSecret);
    }

    @Transactional(readOnly = true)
    public Payment get(MerchantId merchantId, PaymentId paymentId) {
        return paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.value()));
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> list(MerchantId merchantId, Optional<PaymentStatus> status, int page, int size) {
        return paymentRepository.findByMerchantId(merchantId, status, new PageRequest(page, size));
    }

    @Transactional
    public Payment capture(MerchantId merchantId, PaymentId paymentId) {
        Payment payment = paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.value()));
        if (payment.status() == PaymentStatus.CAPTURED) {
            return payment;
        }
        acquiringPort.confirmCapture(paymentId, payment.amount(), merchantId);
        payment.capture(clock.instant());
        paymentRepository.update(payment);
        outboxAppender.append(payment.id().value(), payment.pullDomainEvents());
        return payment;
    }

    @Transactional
    public Payment cancel(MerchantId merchantId, PaymentId paymentId, Optional<String> reason) {
        Payment payment = paymentRepository
                .findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.value()));
        payment.cancel(clock.instant(), reason);
        paymentRepository.update(payment);
        outboxAppender.append(payment.id().value(), payment.pullDomainEvents());
        return payment;
    }
}
