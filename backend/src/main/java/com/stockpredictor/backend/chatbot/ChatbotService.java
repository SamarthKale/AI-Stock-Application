package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.common.ChatbotRateLimitExceededException;
import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import org.springframework.stereotype.Service;

/** Orchestration: per-uid rate-limit check, then delegate to {@link ChatbotClient}. No caching
 *  (unlike {@link com.stockpredictor.backend.prediction.PredictionService}) — chat responses
 *  aren't cacheable, and the backend holds no chat history (that lives only in Android's local
 *  SQLite, per the Phase 5b plan). */
@Service
public class ChatbotService {

    private final ChatbotClient chatbotClient;
    private final ChatbotRateLimiter rateLimiter;

    public ChatbotService(ChatbotClient chatbotClient, ChatbotRateLimiter rateLimiter) {
        this.chatbotClient = chatbotClient;
        this.rateLimiter = rateLimiter;
    }

    public ChatMessageResponseDto sendMessage(String uid, String conversationId, String message) {
        if (!rateLimiter.tryAcquire(uid)) {
            throw new ChatbotRateLimitExceededException("Chatbot message rate limit exceeded — try again later");
        }
        return chatbotClient.sendMessage(conversationId, message);
    }
}
