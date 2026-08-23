package com.stockpredictor.backend.common.dto;

import jakarta.validation.constraints.NotBlank;

/** Android -> Spring Boot request body for POST /api/chatbot/message (Phase 5b). */
public record ChatMessageRequestDto(
        @NotBlank String conversationId,
        @NotBlank String message
) {
}
