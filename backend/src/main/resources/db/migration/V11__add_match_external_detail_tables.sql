CREATE TABLE match_external_detail (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SYNCED', 'FAILED', 'NEEDS_REVIEW')),
    source_url TEXT,
    provider_game_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    summary_json JSONB,
    raw_json JSONB,
    confidence INT NOT NULL DEFAULT 0 CHECK (confidence >= 0 AND confidence <= 100),
    last_synced_at TIMESTAMPTZ,
    error_message TEXT,
    parse_version VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_match_external_detail_provider ON match_external_detail(provider);
CREATE INDEX idx_match_external_detail_status ON match_external_detail(status);

CREATE TABLE match_external_detail_game (
    id BIGSERIAL PRIMARY KEY,
    match_external_detail_id BIGINT NOT NULL REFERENCES match_external_detail(id) ON DELETE CASCADE,
    game_no INT NOT NULL,
    provider_game_id VARCHAR(100),
    duration_sec INT,
    winner_side VARCHAR(10) CHECK (winner_side IN ('BLUE', 'RED')),
    blue_kills INT,
    red_kills INT,
    blue_dragons INT,
    red_dragons INT,
    blue_barons INT,
    red_barons INT,
    blue_bans_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    red_bans_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    blue_picks_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    red_picks_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    gold_timeline_json JSONB,
    objective_timeline_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_match_external_detail_game UNIQUE (match_external_detail_id, game_no)
);

CREATE INDEX idx_match_external_detail_game_detail_id ON match_external_detail_game(match_external_detail_id);
CREATE INDEX idx_match_external_detail_game_provider_game_id ON match_external_detail_game(provider_game_id);
