-- Phase 5: server-side prediction cache. Not user-scoped (the model itself is
-- coin-agnostic-per-user — every user sees the same prediction for a given coin),
-- unlike watchlist/portfolio_holdings. Keyed on (coin_id, horizon) so future
-- multi-horizon support (Phase 5 plan section 1, not implemented yet — "24h" is
-- the only horizon shipped this phase) doesn't require a schema change.
--
-- Redis was deliberately deferred (Phase 5 plan section 10) in favor of this table —
-- see PredictionService.java for the expires_at TTL check.

CREATE TABLE prediction_cache (
    id           BIGSERIAL PRIMARY KEY,
    coin_id      VARCHAR(64)    NOT NULL,
    horizon      VARCHAR(16)    NOT NULL,
    confidence   NUMERIC(5,2)   NOT NULL,
    direction    VARCHAR(8)     NOT NULL,
    target_price NUMERIC(24,8),
    generated_at BIGINT         NOT NULL,
    expires_at   TIMESTAMPTZ    NOT NULL,
    UNIQUE (coin_id, horizon)
);

CREATE INDEX idx_prediction_cache_coin_id ON prediction_cache(coin_id);
