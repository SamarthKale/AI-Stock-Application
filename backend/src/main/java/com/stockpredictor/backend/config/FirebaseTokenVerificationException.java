package com.stockpredictor.backend.config;

/** Thrown for any missing, malformed, expired, or otherwise invalid Firebase ID token. */
public class FirebaseTokenVerificationException extends RuntimeException {
    public FirebaseTokenVerificationException(String message) {
        super(message);
    }

    public FirebaseTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
