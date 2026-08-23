package com.stockpredictor.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Phase 6: one structured log line per request (method, path, status, duration) — every field
 * lands as its own JSON field via logstash-logback-encoder (see logback-spring.xml), not baked
 * into a formatted message string, so `docker compose logs` output is directly grep/jq-able.
 *
 * <p>{@code requestId} and {@code uid} (when authenticated) are pushed onto SLF4J's MDC before
 * the filter chain runs, so every log line emitted anywhere else while handling this request
 * (controllers, services, GlobalExceptionHandler) automatically carries the same correlation
 * fields — no need to pass a request id through every method signature.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Populated only after the security filter chain has run, so this is the first point
            // in the chain where an authenticated uid is actually available.
            String uid = currentUid();
            if (uid != null) {
                MDC.put("uid", uid);
            }
            long durationMs = System.currentTimeMillis() - start;
            log.info("http_request method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.clear();
        }
    }

    private static String currentUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof String uid)) {
            return null;
        }
        return uid;
    }
}
