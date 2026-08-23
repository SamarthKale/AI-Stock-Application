-- Phase 5c: per-(user, coin, rule) alert-send cooldown, so AlertRuleService's scheduled
-- evaluation doesn't spam the same user with the same alert every run. Not user-scoped by a
-- foreign key to users(id) -- uid comes from Firestore (see FirestoreWatchlistReader), not from
-- this backend's own users table, so no FK is meaningful here.

CREATE TABLE alert_cooldowns (
    id           BIGSERIAL PRIMARY KEY,
    user_id      VARCHAR(128) NOT NULL,
    coin_id      VARCHAR(64)  NOT NULL,
    rule_type    VARCHAR(32)  NOT NULL,
    last_sent_at TIMESTAMPTZ  NOT NULL,
    UNIQUE (user_id, coin_id, rule_type)
);

CREATE INDEX idx_alert_cooldowns_user_id ON alert_cooldowns(user_id);
