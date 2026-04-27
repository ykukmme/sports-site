CREATE TABLE gol_gg_tournament_source (
    id BIGSERIAL PRIMARY KEY,
    source_url TEXT NOT NULL UNIQUE,
    label VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SYNCED', 'FAILED')),
    candidate_count INT NOT NULL DEFAULT 0,
    last_indexed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gol_gg_tournament_source_status ON gol_gg_tournament_source(status);

CREATE TABLE gol_gg_indexed_match (
    id BIGSERIAL PRIMARY KEY,
    tournament_source_id BIGINT NOT NULL REFERENCES gol_gg_tournament_source(id) ON DELETE CASCADE,
    provider_game_id VARCHAR(100) NOT NULL UNIQUE,
    source_url TEXT NOT NULL,
    context_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gol_gg_indexed_match_source_id ON gol_gg_indexed_match(tournament_source_id);
CREATE INDEX idx_gol_gg_indexed_match_provider_game_id ON gol_gg_indexed_match(provider_game_id);
