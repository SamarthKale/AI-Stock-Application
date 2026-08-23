package com.stockpredictor.backend.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Manual, opt-in verification that the REAL {@link GeminiChatbotClient} (not
 * {@link FakeChatbotClient}, which every automated test uses instead) actually talks to the real
 * Gemini API over HTTP. Disabled by default — requires a real {@code GEMINI_API_KEY} environment
 * variable and a valid model id (application.yml's {@code chatbot.model} default, overridable via
 * {@code GEMINI_MODEL} — verify the current free-tier model lineup before running this, since it
 * changes over time; gemini-2.0-flash, for instance, is being deprecated). Remove {@code @Disabled}
 * temporarily to re-run this after changing either side of the backend<->Gemini contract, same
 * pattern as FastApiPredictionClientLiveVerification.
 */
@Disabled("Manual verification only — requires a real GEMINI_API_KEY environment variable")
class GeminiChatbotClientLiveVerification {

    @Test
    void realClientAgainstRealGeminiApi() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-flash");
        GeminiChatbotClient client = new GeminiChatbotClient(
                "https://generativelanguage.googleapis.com", apiKey, model);

        ChatMessageResponseDto response = client.sendMessage("live-verification", "Say hello in one short sentence.");

        assertThat(response.conversationId()).isEqualTo("live-verification");
        assertThat(response.reply()).isNotBlank();
    }
}
