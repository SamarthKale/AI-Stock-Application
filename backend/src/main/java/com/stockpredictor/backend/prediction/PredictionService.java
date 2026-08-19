package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.common.dto.PredictionHistoryRequestDto;
import com.stockpredictor.backend.common.dto.PredictionRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Postgres-backed cache in front of {@link PredictionClient} — Redis was deliberately
 *  deferred (Phase 5 plan section 10); this table is the whole caching layer for now. */
@Service
public class PredictionService {

    private static final String HORIZON = "24h";

    private final PredictionCacheRepository cacheRepository;
    private final PredictionClient predictionClient;
    private final Duration cacheTtl;

    public PredictionService(
            PredictionCacheRepository cacheRepository,
            PredictionClient predictionClient,
            @Value("${prediction-service.cache-ttl-minutes:20}") long cacheTtlMinutes) {
        this.cacheRepository = cacheRepository;
        this.predictionClient = predictionClient;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    @Transactional
    public PredictionResponseDto getPrediction(String coinId, PredictionHistoryRequestDto historyRequest) {
        Optional<PredictionCacheEntity> cached = cacheRepository.findByCoinIdAndHorizon(coinId, HORIZON);
        if (cached.isPresent() && cached.get().getExpiresAt().isAfter(Instant.now())) {
            return toDto(cached.get());
        }

        PredictionRequestDto upstreamRequest =
                new PredictionRequestDto(coinId, historyRequest.history(), historyRequest.btcHistory());
        PredictionResponseDto response = predictionClient.predict(upstreamRequest);
        saveToCache(coinId, response, cached.orElse(null));
        return response;
    }

    private void saveToCache(String coinId, PredictionResponseDto response, PredictionCacheEntity existing) {
        PredictionCacheEntity entity = existing != null ? existing : new PredictionCacheEntity();
        entity.setCoinId(coinId);
        entity.setHorizon(HORIZON);
        entity.setConfidence(BigDecimal.valueOf(response.confidence()));
        entity.setDirection(response.direction());
        entity.setTargetPrice(response.targetPrice() != null ? BigDecimal.valueOf(response.targetPrice()) : null);
        entity.setGeneratedAt(response.generatedAt());
        entity.setExpiresAt(Instant.now().plus(cacheTtl));
        cacheRepository.save(entity);
    }

    private PredictionResponseDto toDto(PredictionCacheEntity entity) {
        return new PredictionResponseDto(
                entity.getCoinId(),
                entity.getConfidence().doubleValue(),
                entity.getDirection(),
                entity.getTargetPrice() != null ? entity.getTargetPrice().doubleValue() : null,
                entity.getHorizon(),
                entity.getGeneratedAt());
    }
}
