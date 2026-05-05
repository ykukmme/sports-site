ALTER TABLE match_external_detail_game_override_audit
    ALTER COLUMN field_name TYPE VARCHAR(100);

CREATE TABLE match_external_detail_game_override_distribution_row (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES match_external_detail_game(id) ON DELETE CASCADE,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('GOLD', 'DAMAGE')),
    side VARCHAR(10) NOT NULL CHECK (side IN ('BLUE', 'RED')),
    position VARCHAR(10) NOT NULL CHECK (position IN ('TOP', 'JUNGLE', 'MID', 'ADC', 'SUPPORT')),
    percent NUMERIC(5, 2) NOT NULL CHECK (percent >= 0 AND percent <= 100),
    per_minute NUMERIC(10, 2) NOT NULL CHECK (per_minute >= 0),
    note VARCHAR(500),
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_game_distribution_row_override UNIQUE (game_id, kind, side, position)
);

CREATE INDEX ix_distribution_row_override_game_id
    ON match_external_detail_game_override_distribution_row(game_id);
