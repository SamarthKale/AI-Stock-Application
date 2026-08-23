package com.stockpredictor.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Phase 5c — first scheduled job in this backend ({@link com.stockpredictor.backend.alerts.AlertRuleService}). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
