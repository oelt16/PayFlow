package com.payflow.payment.api.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payment.api.security.MerchantContext;
import com.payflow.payment.application.IdempotencyService;
import com.payflow.payment.application.IdempotencyResult;
import com.payflow.payment.application.exception.IdempotencyKeyReuseException;

/**
 * Filter that handles idempotency key header for API requests.
 * Runs after ApiKeyAuthenticationFilter (order > HIGHEST_PRECEDENCE + 10).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAYED_HEADER = "X-Idempotent-Replayed";
    private static final String IDEMPOTENCY_KEY_ATTR = "idempotency.key";
    private static final String IDEMPOTENCY_HASH_ATTR = "idempotency.hash";
    private static final int MAX_KEY_LENGTH = 64;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Only process /v1/* paths
        if (!request.getRequestURI().startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // No idempotency key - pass through
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate key length
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get merchant from context (set by ApiKeyAuthenticationFilter)
        if (!MerchantContext.isSet()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Cache request body for hash computation
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestHash = idempotencyService.computeHash(cachedRequest.getCachedBody());

        // Lookup existing idempotency key
        try {
            Optional<IdempotencyResult> existing = idempotencyService.lookup(
                    MerchantContext.require(),
                    idempotencyKey,
                    requestHash
            );

            if (existing.isPresent()) {
                // Cache hit - replay cached response
                replayResponse(response, existing.get());
                return;
            }

            // Cache miss - proceed with request
            // Wrap response to capture output
            ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

            // Proceed with cached request and wrapped response
            filterChain.doFilter(cachedRequest, cachingResponse);

            // Capture response and store idempotency key
            captureAndStoreResponse(cachingResponse, idempotencyKey, requestHash);

        } catch (IdempotencyKeyReuseException ex) {
            // Hash mismatch - return 422
            writeErrorResponse(response, request, ex);
        }
    }

    private void replayResponse(HttpServletResponse response, IdempotencyResult result) throws IOException {
        response.setStatus(result.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(IDEMPOTENCY_REPLAYED_HEADER, "true");
        objectMapper.writeValue(response.getOutputStream(), result.responseBody());
    }

    private void writeErrorResponse(HttpServletResponse response, HttpServletRequest request, 
            IdempotencyKeyReuseException ex) throws IOException {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Object requestIdAttr = request.getAttribute("requestId");
        String requestId = requestIdAttr != null ? requestIdAttr.toString() : "unknown";

        Map<String, Object> error = Map.of(
                "code", "idempotency_key_reuse",
                "message", "Idempotency key already used with different request body",
                "requestId", requestId
        );

        objectMapper.writeValue(response.getOutputStream(), Map.of("error", error));
    }

    private void captureAndStoreResponse(
            ContentCachingResponseWrapper cachingResponse,
            String idempotencyKey,
            String requestHash
    ) throws IOException {
        // Get the cached response body
        byte[] responseBytes = cachingResponse.getContentAsByteArray();
        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

        // Parse the JSON response body
        Map<String, Object> responseMap;
        try {
            responseMap = objectMapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse response body for idempotency key: {}", idempotencyKey, e);
            responseMap = Map.of("raw", responseBody);
        }

        // Store the idempotency key with the response
        try {
            idempotencyService.store(
                    MerchantContext.require(),
                    idempotencyKey,
                    requestHash,
                    responseMap,
                    cachingResponse.getStatus()
            );
            log.debug("Stored idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to store idempotency key: {}", idempotencyKey, e);
        }

        // Copy the cached response to the actual response
        cachingResponse.copyBodyToResponse();
    }
}