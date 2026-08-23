package com.stockpredictor.backend.alerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Keyed on (user_id, coin_id, rule_type) -- one row per alert-rule instance per user per coin,
 *  tracking when it last actually sent so AlertCooldownService can suppress a repeat within the
 *  configured cooldown window. */
@Entity
@Table(name = "alert_cooldowns", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coin_id", "rule_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertCooldownEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;
}
