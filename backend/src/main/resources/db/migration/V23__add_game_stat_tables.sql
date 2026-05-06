CREATE TABLE team_game_stat (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    match_external_detail_game_id BIGINT NOT NULL REFERENCES match_external_detail_game(id) ON DELETE CASCADE,
    game_no INTEGER NOT NULL,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    opponent_team_id BIGINT REFERENCES teams(id),
    side VARCHAR(10) NOT NULL,
    win BOOLEAN NOT NULL DEFAULT FALSE,
    kills INTEGER,
    deaths INTEGER,
    gold INTEGER,
    towers INTEGER,
    dragons INTEGER,
    barons INTEGER,
    first_blood BOOLEAN,
    first_tower BOOLEAN,
    duration_sec INTEGER,
    patch_version VARCHAR(20),
    league VARCHAR(50),
    tournament_name VARCHAR(200),
    scheduled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_team_game_stat_game_team UNIQUE (match_external_detail_game_id, team_id)
);

CREATE INDEX idx_team_game_stat_team ON team_game_stat(team_id, scheduled_at DESC);
CREATE INDEX idx_team_game_stat_match ON team_game_stat(match_id);

CREATE TABLE player_game_stat (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    match_external_detail_game_id BIGINT NOT NULL REFERENCES match_external_detail_game(id) ON DELETE CASCADE,
    game_no INTEGER NOT NULL,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    opponent_team_id BIGINT REFERENCES teams(id),
    player_id BIGINT REFERENCES players(id),
    player_name_snapshot VARCHAR(100) NOT NULL,
    position VARCHAR(20),
    side VARCHAR(10) NOT NULL,
    champion_id VARCHAR(100),
    kills INTEGER,
    deaths INTEGER,
    assists INTEGER,
    cs INTEGER,
    vision_score INTEGER,
    wards_placed INTEGER,
    wards_destroyed INTEGER,
    control_wards_purchased INTEGER,
    gd15 INTEGER,
    xpd15 INTEGER,
    csd15 INTEGER,
    gold_share DOUBLE PRECISION,
    damage_share DOUBLE PRECISION,
    win BOOLEAN NOT NULL DEFAULT FALSE,
    duration_sec INTEGER,
    patch_version VARCHAR(20),
    league VARCHAR(50),
    tournament_name VARCHAR(200),
    scheduled_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_player_game_stat_game_player UNIQUE (match_external_detail_game_id, side, position, player_name_snapshot)
);

CREATE INDEX idx_player_game_stat_player ON player_game_stat(player_id, scheduled_at DESC);
CREATE INDEX idx_player_game_stat_team ON player_game_stat(team_id, scheduled_at DESC);
CREATE INDEX idx_player_game_stat_name ON player_game_stat(player_name_snapshot);
CREATE INDEX idx_player_game_stat_match ON player_game_stat(match_id);
