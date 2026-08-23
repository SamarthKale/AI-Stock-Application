package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.common.dto.ChatMessageRequestDto;
import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Falls under {@code SecurityConfig}'s existing {@code anyRequest().authenticated()} catch-all —
 * no SecurityConfig change needed (same as prediction/watchlist/portfolio). The verified uid is
 * used only as {@link ChatbotRateLimiter}'s per-user key, not to scope any stored data — this
 * proxy is stateless; chat history lives only in Android's local SQLite (Phase 5b plan).
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ChatMessageResponseDto sendMessage(
            Authentication authentication,
            @Valid @RequestBody ChatMessageRequestDto request) {
        return chatbotService.sendMessage(uid(authentication), request.conversationId(), request.message());
    }

    /** The current user's id always comes from the verified token — never from a client-supplied
     *  parameter (same pattern as WatchlistController). */
    private static String uid(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
