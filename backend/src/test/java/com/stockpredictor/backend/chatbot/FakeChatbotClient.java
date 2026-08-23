package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.common.ChatbotServiceUnavailableException;
import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import java.util.concurrent.atomic.AtomicInteger;

/** Test double avoiding the need for a live Gemini API call during backend tests — same
 *  reasoning as FakePredictionClient avoiding a live FastAPI process. */
public class FakeChatbotClient implements ChatbotClient {

    public static final String UNAVAILABLE_MESSAGE = "trigger-unavailable";

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public ChatMessageResponseDto sendMessage(String conversationId, String message) {
        callCount.incrementAndGet();
        if (UNAVAILABLE_MESSAGE.equals(message)) {
            throw new ChatbotServiceUnavailableException("Chatbot service is unavailable", null);
        }
        return new ChatMessageResponseDto("Fake reply to: " + message, conversationId);
    }

    public int callCount() {
        return callCount.get();
    }
}
