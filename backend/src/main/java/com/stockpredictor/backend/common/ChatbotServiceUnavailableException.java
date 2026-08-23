package com.stockpredictor.backend.common;

/**
 * Thrown when the Gemini chatbot API is unreachable, times out, errors, or returns an
 * unparseable response — mapped to 503, never propagated as a raw 5xx or leaked provider error
 * text. Mirrors {@link PredictionServiceUnavailableException}'s exact role for ai-service.
 */
public class ChatbotServiceUnavailableException extends RuntimeException {
    public ChatbotServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
