package com.stockpredictor.backend.alerts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production {@link MarketDataClient} — a single batched {@code GET /coins/markets?ids=...} call
 * to CoinGecko per scheduled run (not one call per user, not one per coin), via Spring's
 * {@link RestClient} (already available, no new Gradle dependency — same precedent as
 * FastApiPredictionClient/GeminiChatbotClient). This is the ONLY place in the backend that talks
 * to CoinGecko.
 */
@Component
public class CoinGeckoMarketDataClient implements MarketDataClient {

    private final RestClient restClient;

    public CoinGeckoMarketDataClient(
            @Value("${coingecko.base-url}") String baseUrl,
            @Value("${coingecko.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-cg-demo-api-key", apiKey)
                .build();
    }

    @Override
    public Map<String, CoinMarketSnapshot> getMarketSnapshots(Set<String> coinIds) {
        if (coinIds.isEmpty()) {
            return Map.of();
        }
        try {
            String ids = String.join(",", coinIds);
            CoinMarketDto[] response = restClient.get()
                    .uri("/coins/markets?vs_currency=usd&ids={ids}", ids)
                    .retrieve()
                    .body(CoinMarketDto[].class);
            if (response == null) {
                return Map.of();
            }
            return Arrays.stream(response)
                    .filter(dto -> dto.id() != null)
                    .collect(Collectors.toMap(
                            CoinMarketDto::id,
                            dto -> new CoinMarketSnapshot(dto.id(), dto.name(), dto.currentPrice(), dto.priceChangePercentage24h()),
                            (first, second) -> first));
        } catch (RestClientException e) {
            throw new AlertDataUnavailableException("Failed to fetch CoinGecko market data", e);
        }
    }

    private record CoinMarketDto(
            String id,
            String name,
            @JsonProperty("current_price") Double currentPrice,
            @JsonProperty("price_change_percentage_24h") Double priceChangePercentage24h) {
    }
}
