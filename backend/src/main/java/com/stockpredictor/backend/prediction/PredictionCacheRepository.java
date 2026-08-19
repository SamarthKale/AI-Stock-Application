package com.stockpredictor.backend.prediction;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionCacheRepository extends JpaRepository<PredictionCacheEntity, Long> {

    Optional<PredictionCacheEntity> findByCoinIdAndHorizon(String coinId, String horizon);
}
