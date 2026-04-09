package com.payflow.payment.api;

import com.payflow.payment.api.dto.CancelPaymentRequest;
import com.payflow.payment.api.dto.CreatePaymentRequest;
import com.payflow.payment.api.dto.CreateRefundRequest;
import com.payflow.payment.api.dto.PaymentListResponse;
import com.payflow.payment.api.dto.PaymentResponse;
import com.payflow.payment.api.dto.RefundListResponse;
import com.payflow.payment.api.dto.RefundResponse;
import com.payflow.payment.api.security.MerchantContext;
import com.payflow.payment.application.CreatePaymentCommand;
import com.payflow.payment.application.CreatedPaymentResult;
import com.payflow.payment.application.PaymentApplicationService;
import com.payflow.payment.domain.PaymentId;
import com.payflow.payment.domain.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@Validated
public class PaymentsController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentsController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest body) {
        CreatePaymentCommand command = new CreatePaymentCommand(
                body.getAmount(),
                body.getCurrency(),
                body.getDescription(),
                body.getCard().getNumber(),
                body.getCard().getExpMonth(),
                body.getCard().getExpYear(),
                body.getCard().getCvc(),
                body.getMetadata()
        );
        CreatedPaymentResult result = paymentApplicationService.create(MerchantContext.require(), command);
        PaymentResponse response = PaymentApiMapper.toResponse(result.payment(), result.clientSecret());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable String id) {
        return PaymentApiMapper.toResponse(
                paymentApplicationService.get(MerchantContext.require(), PaymentId.of(id))
        );
    }

    @GetMapping
    public PaymentListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        Optional<PaymentStatus> statusFilter = Optional.ofNullable(status)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PaymentStatus::valueOf);
        var pageResult = paymentApplicationService.list(MerchantContext.require(), statusFilter, page, size);
        List<PaymentResponse> content = pageResult.content().stream()
                .map(PaymentApiMapper::toResponse)
                .collect(Collectors.toList());
        return new PaymentListResponse(content, pageResult.totalElements(), pageResult.page(), pageResult.size());
    }

    @PostMapping("/{id}/capture")
    public PaymentResponse capture(@PathVariable String id) {
        return PaymentApiMapper.toResponse(
                paymentApplicationService.capture(MerchantContext.require(), PaymentId.of(id))
        );
    }

    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(
            @PathVariable String id,
            @RequestBody(required = false) CancelPaymentRequest body
    ) {
        Optional<String> reason = Optional.ofNullable(body)
                .flatMap(b -> Optional.ofNullable(b.getReason()))
                .map(String::trim)
                .filter(s -> !s.isEmpty());
        return PaymentApiMapper.toResponse(
                paymentApplicationService.cancel(MerchantContext.require(), PaymentId.of(id), reason)
        );
    }

    @PostMapping("/{id}/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @PathVariable String id,
            @Valid @RequestBody CreateRefundRequest body
    ) {
        var refund = paymentApplicationService.refund(
                MerchantContext.require(),
                PaymentId.of(id),
                body.getAmount(),
                body.getCurrency(),
                Optional.ofNullable(body.getReason())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundApiMapper.toResponse(refund));
    }

    @GetMapping("/{id}/refunds")
    public RefundListResponse listRefunds(@PathVariable String id) {
        var list = paymentApplicationService.listRefunds(MerchantContext.require(), PaymentId.of(id));
        List<RefundResponse> data = list.stream().map(RefundApiMapper::toResponse).collect(Collectors.toList());
        return new RefundListResponse(data, data.size());
    }
}
