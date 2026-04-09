package com.payflow.webhook.api;

import com.payflow.webhook.api.security.RequestIdFilter;
import com.payflow.webhook.application.exception.WebhookNotFoundException;
import com.payflow.webhook.domain.DomainException;
import com.payflow.webhook.domain.InvalidWebhookUrlException;
import com.payflow.webhook.domain.MaxWebhookEndpointsExceededException;

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

    @ExceptionHandler(WebhookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(WebhookNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "webhook_not_found", ex.getMessage(), "id", request);
    }

    @ExceptionHandler(InvalidWebhookUrlException.class)
    public ResponseEntity<Map<String, Object>> badUrl(InvalidWebhookUrlException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "invalid_webhook_url", ex.getMessage(), "url", request);
    }

    @ExceptionHandler(MaxWebhookEndpointsExceededException.class)
    public ResponseEntity<Map<String, Object>> maxEndpoints(
            MaxWebhookEndpointsExceededException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "max_webhooks_exceeded", ex.getMessage(), null, request);
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
