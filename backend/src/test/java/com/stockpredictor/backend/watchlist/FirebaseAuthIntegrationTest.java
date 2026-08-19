package com.stockpredictor.backend.watchlist;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpredictor.backend.common.dto.ErrorResponse;
import com.stockpredictor.backend.common.dto.WatchlistItemDto;
import com.stockpredictor.backend.config.FakeFirebaseTokenVerifier;
import com.stockpredictor.backend.config.TestSecurityBeans;
import com.stockpredictor.backend.user.UserRepository;
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
 * Phase 3's acceptance-criterion integration test: exercises a protected endpoint
 * (GET /api/watchlist) with a valid token, an expired token, and no token — running against the
 * real Spring Security filter chain, FirebaseAuthenticationFilter, and a real local Postgres
 * database (see application-test.yml), with only the Firebase Admin SDK call itself faked
 * (see FakeFirebaseTokenVerifier) since a live Firebase project isn't available in this run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class FirebaseAuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @BeforeEach
    void cleanDatabase() {
        watchlistRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void missingToken_isRejectedWith401() {
        var response = restTemplate.getForEntity("/api/watchlist", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    void expiredToken_isRejectedWith401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FakeFirebaseTokenVerifier.EXPIRED_TOKEN);

        var response = restTemplate.exchange(
                "/api/watchlist", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_isAcceptedAndProvisionsUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(FakeFirebaseTokenVerifier.VALID_TOKEN);

        var response = restTemplate.exchange(
                "/api/watchlist", HttpMethod.GET, new HttpEntity<>(headers), WatchlistItemDto[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
        assertThat(userRepository.existsById(FakeFirebaseTokenVerifier.VALID_TOKEN_UID)).isTrue();
    }
}
