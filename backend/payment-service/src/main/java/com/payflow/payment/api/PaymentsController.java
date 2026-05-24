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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Payments", description = "Payment lifecycle — create, capture, cancel, refund, and manage refunds")
public class PaymentsController {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentsController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create a payment", description = "Creates a new payment with card details. Requires idempotency key for safe retries.")
    @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Idempotency key for safe retries (required)", required = true, schema = @Schema(type = "string"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest body
    ) {
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
    @Operation(summary = "Get a payment by ID", description = "Retrieves payment details for a given payment ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public PaymentResponse get(@PathVariable String id) {
        return PaymentApiMapper.toResponse(
                paymentApplicationService.get(MerchantContext.require(), PaymentId.of(id))
        );
    }

    @GetMapping
    @Operation(summary = "List payments", description = "Lists payments for the authenticated merchant with pagination and optional status filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of payments", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
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
    @Operation(summary = "Capture a payment", description = "Captures an authorized but uncaptured payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment captured", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public PaymentResponse capture(@PathVariable String id) {
        return PaymentApiMapper.toResponse(
                paymentApplicationService.capture(MerchantContext.require(), PaymentId.of(id))
        );
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a payment", description = "Cancels a pending payment with an optional reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment cancelled", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
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
    @Operation(summary = "Create a refund", description = "Creates a refund for a captured payment. Requires idempotency key for safe retries.")
    @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Idempotency key for safe retries", required = false, schema = @Schema(type = "string"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refund created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
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
    @Operation(summary = "List refunds for a payment", description = "Lists all refunds associated with a given payment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of refunds", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Unauthorized — invalid or missing API key", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ApiErrorResponse.class)))
    })
    public RefundListResponse listRefunds(@PathVariable String id) {
        var list = paymentApplicationService.listRefunds(MerchantContext.require(), PaymentId.of(id));
        List<RefundResponse> data = list.stream().map(RefundApiMapper::toResponse).collect(Collectors.toList());
        return new RefundListResponse(data, data.size());
    }
}
