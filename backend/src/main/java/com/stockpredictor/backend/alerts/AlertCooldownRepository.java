package com.stockpredictor.backend.alerts;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertCooldownRepository extends JpaRepository<AlertCooldownEntity, Long> {
    Optional<AlertCooldownEntity> findByUserIdAndCoinIdAndRuleType(String userId, String coinId, String ruleType);
}
