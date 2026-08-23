package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;

/** Seam over "which LLM/chatbot provider the backend calls" — mirrors the
 *  {@link com.stockpredictor.backend.prediction.PredictionClient} interface+impl pattern (itself
 *  mirroring FirebaseTokenVerifier/FirebaseAdminTokenVerifier) so tests can substitute a fake
 *  without a live Gemini API call. */
public interface ChatbotClient {

    /** @throws com.stockpredictor.backend.common.ChatbotServiceUnavailableException if the
     *  provider is unreachable, times out, errors, or returns an unparseable response — never a
     *  raw network exception. */
    ChatMessageResponseDto sendMessage(String conversationId, String message);
}
