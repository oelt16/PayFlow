package com.payflow.webhook.api.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "payflowRequestId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String rid = request.getHeader("X-Request-Id");
        if (rid == null || rid.isBlank()) {
            rid = "req_" + UUID.randomUUID().toString().replace("-", "");
        }
        request.setAttribute(REQUEST_ID_ATTRIBUTE, rid);
        response.setHeader("X-Request-Id", rid);
        filterChain.doFilter(request, response);
    }
}
