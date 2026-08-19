package com.stockpredictor.backend.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Not user-scoped — predictions are the same for every user viewing a given coin (Phase 5
 *  plan section 8/10). Keyed on (coin_id, horizon); only "24h" is populated this phase. */
@Entity
@Table(name = "prediction_cache", uniqueConstraints = @UniqueConstraint(columnNames = {"coin_id", "horizon"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredictionCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(nullable = false)
    private String horizon;

    @Column(nullable = false)
    private BigDecimal confidence;

    @Column(nullable = false)
    private String direction;

    @Column(name = "target_price")
    private BigDecimal targetPrice;

    @Column(name = "generated_at", nullable = false)
    private Long generatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
