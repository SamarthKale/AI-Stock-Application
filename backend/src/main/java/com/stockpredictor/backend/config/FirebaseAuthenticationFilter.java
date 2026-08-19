package com.stockpredictor.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpredictor.backend.common.dto.ErrorResponse;
import com.stockpredictor.backend.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the Authorization: Bearer <Firebase ID token> header on every request to a protected
 * path, via the injected {@link FirebaseTokenVerifier} (the real Admin SDK implementation in
 * production, a fake in tests). On success, the authenticated principal is always the verified
 * uid from the token — a uid/user_id passed in a request body or param is never trusted (see
 * WatchlistController/PortfolioController, which read it exclusively from SecurityContextHolder).
 *
 * On any missing/invalid/expired token, writes a 401 with the shared ErrorResponse body directly
 * and does not continue the filter chain.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public FirebaseAuthenticationFilter(FirebaseTokenVerifier tokenVerifier, UserService userService, ObjectMapper objectMapper) {
        this.tokenVerifier = tokenVerifier;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, request, "Missing bearer token");
            return;
        }
        String idToken = header.substring("Bearer ".length()).trim();
        if (idToken.isEmpty()) {
            writeUnauthorized(response, request, "Missing bearer token");
            return;
        }

        try {
            FirebaseUserPrincipal principal = tokenVerifier.verify(idToken);
            userService.ensureUserExists(principal);
            var authentication = new UsernamePasswordAuthenticationToken(principal.uid(), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (FirebaseTokenVerificationException e) {
            writeUnauthorized(response, request, e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
