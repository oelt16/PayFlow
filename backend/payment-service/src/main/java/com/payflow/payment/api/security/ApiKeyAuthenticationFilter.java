package com.payflow.payment.api.security;

import com.payflow.payment.domain.MerchantId;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final Map<String, MerchantId> keyToMerchant;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(PayflowSecurityProperties properties, ObjectMapper objectMapper) {
        this.keyToMerchant = properties.getApiKeys().stream()
                .filter(e -> e.getKey() != null && e.getMerchantId() != null)
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().trim(), e -> MerchantId.of(e.getMerchantId().trim())));
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            writeUnauthorized(response, request, "invalid_api_key", "Missing or invalid Authorization header");
            return;
        }
        String token = auth.substring("Bearer ".length()).trim();
        MerchantId merchantId = keyToMerchant.get(token);
        if (merchantId == null) {
            writeUnauthorized(response, request, "invalid_api_key", "Unknown API key");
            return;
        }
        try {
            MerchantContext.set(merchantId);
            filterChain.doFilter(request, response);
        } finally {
            MerchantContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String code, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        objectMapper.writeValue(
                response.getOutputStream(),
                Map.of(
                        "error",
                        Map.of(
                                "code", code,
                                "message", message,
                                "requestId", requestId
                        )
                )
        );
    }
}
