package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.common.ChatbotServiceUnavailableException;
import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production {@link ChatbotClient} — calls Google Gemini's generateContent REST API via Spring's
 * {@link RestClient} (already available from spring-boot-starter-web, no new Gradle dependency —
 * same precedent as {@link com.stockpredictor.backend.prediction.FastApiPredictionClient}). This
 * is the ONLY place in the backend that talks to Gemini; the API key never leaves this class —
 * read from environment config via {@code @Value} (application.yml's {@code chatbot.api-key},
 * backed by {@code GEMINI_API_KEY}) and sent as the {@code x-goog-api-key} HEADER rather than a
 * URL query parameter, so it can never end up in a logged request URI.
 *
 * {@code chatbot.model} (backed by {@code GEMINI_MODEL}) is intentionally not hardcoded here —
 * Gemini's free-tier model lineup changes over time (confirmed live before implementation: the
 * originally-considered gemini-2.0-flash is being deprecated), so the exact model id is an
 * externally configurable value, not a compiled-in constant.
 */
@Component
public class GeminiChatbotClient implements ChatbotClient {

    private final RestClient restClient;
    private final String model;

    public GeminiChatbotClient(
            @Value("${chatbot.base-url}") String baseUrl,
            @Value("${chatbot.api-key}") String apiKey,
            @Value("${chatbot.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Override
    public ChatMessageResponseDto sendMessage(String conversationId, String message) {
        try {
            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiContent("user", List.of(new GeminiPart(message)))));
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
            return new ChatMessageResponseDto(extractReply(response), conversationId);
        } catch (RestClientException e) {
            throw new ChatbotServiceUnavailableException("Chatbot service is unavailable", e);
        }
    }

    private static String extractReply(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new ChatbotServiceUnavailableException("Chatbot service returned no candidates", null);
        }
        GeminiContent content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new ChatbotServiceUnavailableException("Chatbot service returned an empty reply", null);
        }
        return content.parts().get(0).text();
    }

    // Gemini's REST request/response shapes — internal to this client only. Android never sees
    // these; it only ever sees ChatMessageResponseDto.
    private record GeminiRequest(List<GeminiContent> contents) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }
}
