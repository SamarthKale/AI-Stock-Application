package com.stockpredictor.backend.prediction;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpredictor.backend.common.dto.ErrorResponse;
import com.stockpredictor.backend.common.dto.PredictionHistoryRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import com.stockpredictor.backend.common.dto.PricePointDto;
import com.stockpredictor.backend.config.FakeFirebaseTokenVerifier;
import com.stockpredictor.backend.config.TestSecurityBeans;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises POST /api/predictions/{coinId} against the real Spring Security filter chain and a
 * real local Postgres database, with only the Firebase Admin SDK call and the FastAPI call
 * faked (FakeFirebaseTokenVerifier, FakePredictionClient) — same pattern as Phase 3's
 * FirebaseAuthIntegrationTest, extended to also fake the new external dependency this phase adds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestSecurityBeans.class, TestPredictionBeans.class})
class PredictionControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PredictionCacheRepository predictionCacheRepository;

    @Autowired
    private PredictionClient predictionClient;

    @BeforeEach
    void cleanDatabase() {
        predictionCacheRepository.deleteAll();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FakeFirebaseTokenVerifier.VALID_TOKEN);
        return headers;
    }

    private PredictionHistoryRequestDto sampleRequest() {
        List<PricePointDto> history = List.of(
                new PricePointDto(1_699_000_000_000L, 60000.0, 1_000_000.0),
                new PricePointDto(1_699_086_400_000L, 61000.0, 1_100_000.0));
        return new PredictionHistoryRequestDto(history, history);
    }

    @Test
    void missingToken_isRejectedWith401() {
        var response = restTemplate.postForEntity(
                "/api/predictions/bitcoin", new HttpEntity<>(sampleRequest()), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_returnsPredictionFromClient() {
        var entity = new HttpEntity<>(sampleRequest(), authHeaders());
        var response = restTemplate.exchange(
                "/api/predictions/bitcoin", HttpMethod.POST, entity, PredictionResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().coinId()).isEqualTo("bitcoin");
        assertThat(response.getBody().direction()).isEqualTo("UP");
        assertThat(response.getBody().horizon()).isEqualTo("24h");
    }

    @Test
    void predictionServiceUnavailable_isMapped503NotRaw5xx() {
        var entity = new HttpEntity<>(sampleRequest(), authHeaders());
        var response = restTemplate.exchange(
                "/api/predictions/" + FakePredictionClient.UNAVAILABLE_COIN_ID,
                HttpMethod.POST, entity, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    void secondRequestWithinTtl_servesFromCache_doesNotCallPredictionClientAgain() {
        FakePredictionClient fake = (FakePredictionClient) predictionClient;
        int before = fake.callCount();

        var entity = new HttpEntity<>(sampleRequest(), authHeaders());
        restTemplate.exchange("/api/predictions/ethereum", HttpMethod.POST, entity, PredictionResponseDto.class);
        restTemplate.exchange("/api/predictions/ethereum", HttpMethod.POST, entity, PredictionResponseDto.class);

        int after = fake.callCount();
        assertThat(after - before).isEqualTo(1);
    }
}
