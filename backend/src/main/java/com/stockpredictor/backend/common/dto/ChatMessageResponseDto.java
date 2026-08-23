package com.stockpredictor.backend.common.dto;

/** Spring Boot -> Android response body — matches CLAUDE.md's documented
 *  {reply, conversationId} chatbot contract exactly. */
public record ChatMessageResponseDto(String reply, String conversationId) {
}
