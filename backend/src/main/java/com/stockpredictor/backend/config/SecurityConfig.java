package com.stockpredictor.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpredictor.backend.common.dto.ErrorResponse;
import com.stockpredictor.backend.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every /api/** endpoint requires a verified Firebase ID token; only /actuator/health is public.
 * Auth is delegated entirely to {@link FirebaseAuthenticationFilter} — Firebase is the single
 * source of identity, this backend never issues its own JWT (see FirebaseAdminTokenVerifier).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public FirebaseAuthenticationFilter firebaseAuthenticationFilter(
            FirebaseTokenVerifier tokenVerifier, UserService userService, ObjectMapper objectMapper) {
        return new FirebaseAuthenticationFilter(tokenVerifier, userService, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, FirebaseAuthenticationFilter firebaseAuthenticationFilter,
                                            ObjectMapper objectMapper) throws Exception {
        http
                // Stateless bearer-token REST API — no browser session/cookie to forge, so CSRF
                // protection (designed for cookie-based auth) doesn't apply here.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Defense in depth: FirebaseAuthenticationFilter already short-circuits every
                // missing/invalid token with its own 401 body, so this rarely fires — but it keeps
                // the same ErrorResponse shape if a future protected path ever reaches here unauthenticated.
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorResponse body = ErrorResponse.of(
                            HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Authentication required", request.getRequestURI());
                    objectMapper.writeValue(response.getWriter(), body);
                }));
        return http.build();
    }
}
