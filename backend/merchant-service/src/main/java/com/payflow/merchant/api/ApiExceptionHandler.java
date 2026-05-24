package com.payflow.merchant.api;

import com.payflow.merchant.api.security.RequestIdFilter;
import com.payflow.merchant.application.exception.MerchantNotFoundException;
import com.payflow.merchant.domain.exception.DomainException;
import com.payflow.merchant.domain.exception.DuplicateEmailException;
import com.payflow.merchant.domain.exception.MerchantAlreadyDeactivatedException;

import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @Schema(description = "Standard error response body")
    public record ApiErrorResponse(@Schema(description = "Error details") ErrorDetail error) {

        public record ErrorDetail(
                @Schema(description = "Error code identifier", example = "merchant_not_found") String code,
                @Schema(description = "Human-readable error message", example = "Merchant not found") String message,
                @Schema(description = "Request ID for traceability", example = "req-abc123") String requestId,
                @Schema(description = "Parameter that caused the error, if applicable", example = "id", nullable = true) String param
        ) {}
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(MerchantNotFoundException ex, HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "merchant_not_found",
                ex.getMessage(),
                "id",
                request
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> duplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "duplicate_email", ex.getMessage(), "email", request);
    }

    @ExceptionHandler(MerchantAlreadyDeactivatedException.class)
    public ResponseEntity<ApiErrorResponse> alreadyDeactivated(
            MerchantAlreadyDeactivatedException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "merchant_already_deactivated", ex.getMessage(), null, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", ex.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String param = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getField)
                .orElse(null);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        return error(HttpStatus.BAD_REQUEST, "validation_error", message, param, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> notReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_json", "Malformed JSON body", null, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> domain(DomainException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "domain_error", ex.getMessage(), null, request);
    }

    private static ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            String param,
            HttpServletRequest request
    ) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(code, message, requestId, param))
        );
    }
}
