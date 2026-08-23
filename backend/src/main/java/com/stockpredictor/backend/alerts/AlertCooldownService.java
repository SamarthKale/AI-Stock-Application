package com.stockpredictor.backend.alerts;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Postgres-backed per-(user, coin, rule) cooldown -- the Phase 5c DoD's dedup requirement.
 *  {@link #isInCooldown} is a pure check; {@link #recordSent} is called only after PushSender
 *  actually succeeds, so a failed send never gets silently swallowed by an unwarranted cooldown. */
@Service
public class AlertCooldownService {

    private final AlertCooldownRepository repository;
    private final Duration cooldown;

    public AlertCooldownService(
            AlertCooldownRepository repository,
            @Value("${alerts.cooldown-minutes:60}") long cooldownMinutes) {
        this.repository = repository;
        this.cooldown = Duration.ofMinutes(cooldownMinutes);
    }

    @Transactional(readOnly = true)
    public boolean isInCooldown(String userId, String coinId, String ruleType) {
        Optional<AlertCooldownEntity> existing = repository.findByUserIdAndCoinIdAndRuleType(userId, coinId, ruleType);
        return existing.map(entity -> entity.getLastSentAt().plus(cooldown).isAfter(Instant.now())).orElse(false);
    }

    @Transactional
    public void recordSent(String userId, String coinId, String ruleType) {
        AlertCooldownEntity entity = repository.findByUserIdAndCoinIdAndRuleType(userId, coinId, ruleType)
                .orElseGet(AlertCooldownEntity::new);
        entity.setUserId(userId);
        entity.setCoinId(coinId);
        entity.setRuleType(ruleType);
        entity.setLastSentAt(Instant.now());
        repository.save(entity);
    }
}
