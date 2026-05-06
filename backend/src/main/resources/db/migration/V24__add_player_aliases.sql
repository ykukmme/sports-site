CREATE TABLE player_alias (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    alias_name VARCHAR(100) NOT NULL,
    normalized_alias VARCHAR(100) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_player_alias_player_normalized UNIQUE (player_id, normalized_alias)
);

CREATE INDEX idx_player_alias_normalized ON player_alias(normalized_alias);
CREATE INDEX idx_player_alias_player ON player_alias(player_id);
