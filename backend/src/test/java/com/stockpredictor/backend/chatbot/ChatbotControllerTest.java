package com.stockpredictor.backend.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpredictor.backend.common.dto.ChatMessageRequestDto;
import com.stockpredictor.backend.common.dto.ChatMessageResponseDto;
import com.stockpredictor.backend.common.dto.ErrorResponse;
import com.stockpredictor.backend.config.FakeFirebaseTokenVerifier;
import com.stockpredictor.backend.config.TestSecurityBeans;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises POST /api/chatbot/message against the real Spring Security filter chain, with only
 * the Firebase Admin SDK call and the Gemini call faked (FakeFirebaseTokenVerifier,
 * FakeChatbotClient) — same pattern as PredictionControllerTest, extended for this phase's new
 * external dependency (Gemini) and its rate limiter.
 *
 * FakeFirebaseTokenVerifier only recognizes one valid token/uid, shared by every test in this
 * class (and every other controller test class) — so ChatbotRateLimiter's per-uid quota is a
 * single shared bucket across every @Test method here (the singleton bean persists for the whole
 * class, no @DirtiesContext). @TestMethodOrder pins the quota-exhausting rate-limit test last so
 * it can never leave the shared uid rate-limited for its siblings.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestSecurityBeans.class, TestChatbotBeans.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatbotControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FakeFirebaseTokenVerifier.VALID_TOKEN);
        return headers;
    }

    @Test
    @Order(1)
    void missingToken_isRejectedWith401() {
        var request = new ChatMessageRequestDto("conversation-1", "hello");
        var response = restTemplate.postForEntity("/api/chatbot/message", new HttpEntity<>(request), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(2)
    void validToken_returnsReplyFromClient() {
        var request = new ChatMessageRequestDto("conversation-2", "What is Bitcoin?");
        var entity = new HttpEntity<>(request, authHeaders());
        var response = restTemplate.exchange("/api/chatbot/message", HttpMethod.POST, entity, ChatMessageResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().conversationId()).isEqualTo("conversation-2");
        assertThat(response.getBody().reply()).isNotBlank();
    }

    @Test
    @Order(3)
    void chatbotServiceUnavailable_isMapped503NotRaw5xx() {
        var request = new ChatMessageRequestDto("conversation-3", FakeChatbotClient.UNAVAILABLE_MESSAGE);
        var entity = new HttpEntity<>(request, authHeaders());
        var response = restTemplate.exchange("/api/chatbot/message", HttpMethod.POST, entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    @Order(4)
    void missingMessage_isRejectedWith400() {
        var request = new ChatMessageRequestDto("conversation-4", "");
        var entity = new HttpEntity<>(request, authHeaders());
        var response = restTemplate.exchange("/api/chatbot/message", HttpMethod.POST, entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(5)
    void exceedingRateLimit_isMapped429() {
        // application-test.yml sets chatbot.rate-limit-per-hour to a small value (3) specifically
        // so this test doesn't need 20+ requests. @Order(5) (highest in this class) guarantees
        // this runs last, so exhausting the shared uid's quota here can't rate-limit any sibling
        // test. Looping until 429 actually appears (rather than asserting an exact call count)
        // keeps the test correct regardless of exactly how much quota Orders 2-4 consumed.
        var request = new ChatMessageRequestDto("conversation-5", "ping");
        var entity = new HttpEntity<>(request, authHeaders());

        HttpStatusCode lastStatus = null;
        for (int i = 0; i < 50; i++) {
            var response = restTemplate.exchange("/api/chatbot/message", HttpMethod.POST, entity, ErrorResponse.class);
            lastStatus = response.getStatusCode();
            if (lastStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                break;
            }
        }

        assertThat(lastStatus).isNotNull();
        assertThat(lastStatus.value()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}
