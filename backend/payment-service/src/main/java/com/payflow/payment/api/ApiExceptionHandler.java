package com.payflow.payment.api;

import com.payflow.payment.api.security.RequestIdFilter;
import com.payflow.payment.application.exception.PaymentNotFoundException;
import com.payflow.payment.domain.exception.DomainException;
import com.payflow.payment.domain.exception.InvalidCurrencyException;
import com.payflow.payment.domain.exception.InvalidStateTransitionException;
import com.payflow.payment.domain.exception.NegativeAmountException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(PaymentNotFoundException ex, HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "payment_not_found",
                ex.getMessage(),
                "id",
                request
        );
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> invalidState(InvalidStateTransitionException ex, HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "invalid_state_transition",
                ex.getMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<Map<String, Object>> invalidCurrency(InvalidCurrencyException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_currency", ex.getMessage(), "currency", request);
    }

    @ExceptionHandler(NegativeAmountException.class)
    public ResponseEntity<Map<String, Object>> negativeAmount(NegativeAmountException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_amount", ex.getMessage(), "amount", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> illegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request", ex.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
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
    public ResponseEntity<Map<String, Object>> notReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_json", "Malformed JSON body", null, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> domain(DomainException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "domain_error", ex.getMessage(), null, request);
    }

    private static ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String code,
            String message,
            String param,
            HttpServletRequest request
    ) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("message", message);
        err.put("requestId", requestId);
        if (param != null) {
            err.put("param", param);
        }
        Map<String, Object> body = Map.of("error", err);
        return ResponseEntity.status(status).body(body);
    }
}
