package com.stockpredictor.backend.common.dto;

import java.time.Instant;
import java.util.Map;

/**
 * The single error response shape for every failure path in the backend — no endpoint ever
 * returns raw Postgres/Hibernate/stack-trace text to a client.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse ofFieldErrors(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
