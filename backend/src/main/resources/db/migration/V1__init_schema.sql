-- Phase 3 initial schema. Mirrors the SQLite (Phase 2) / Firestore (Phase 2.5) shapes so a
-- future three-way sync maps cleanly, per CLAUDE.md's Phase 3 guidance.
--
-- users.id is a VARCHAR, not a native Postgres UUID: Firebase UIDs are opaque ~28-char
-- alphanumeric strings, not RFC4122 UUIDs, even though CLAUDE.md's schema sketch calls the
-- column "uuid". Using a native UUID column here would reject every real Firebase UID.

CREATE TABLE users (
    id           VARCHAR(128) PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE watchlist (
    id         BIGSERIAL PRIMARY KEY,
    user_id    VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol     VARCHAR(32)  NOT NULL,
    added_at   BIGINT       NOT NULL,
    sort_order INT          NOT NULL,
    UNIQUE (user_id, symbol)
);

CREATE INDEX idx_watchlist_user_id ON watchlist(user_id);

CREATE TABLE portfolio_holdings (
    id            BIGSERIAL PRIMARY KEY,
    user_id       VARCHAR(128)   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol        VARCHAR(32)    NOT NULL,
    quantity      NUMERIC(18,6)  NOT NULL,
    avg_buy_price NUMERIC(18,4)  NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_portfolio_user_id ON portfolio_holdings(user_id);
