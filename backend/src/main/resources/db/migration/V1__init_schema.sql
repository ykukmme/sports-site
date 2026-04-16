-- Phase 1 초기 스키마 생성
-- Flyway 마이그레이션 V1 — games, teams, players, matches, match_results

-- =====================
-- Task 13: games, teams
-- =====================

CREATE TABLE games (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL UNIQUE,
    short_name  VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE teams (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    short_name  VARCHAR(20),
    region      VARCHAR(50),
    logo_url    TEXT,
    external_id VARCHAR(100)    UNIQUE,
    game_id     BIGINT          NOT NULL REFERENCES games(id),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teams_game_id ON teams(game_id);
CREATE INDEX idx_teams_external_id ON teams(external_id);

-- =============================================
-- Task 14: players, matches, match_results
-- =============================================

CREATE TABLE players (
    id                  BIGSERIAL       PRIMARY KEY,
    in_game_name        VARCHAR(100)    NOT NULL,
    real_name           VARCHAR(100),
    role                VARCHAR(50),
    nationality         VARCHAR(50),
    profile_image_url   TEXT,
    -- team_id nullable: 팀 미소속 선수 허용
    team_id             BIGINT          REFERENCES teams(id),
    external_id         VARCHAR(100)    UNIQUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_team_id ON players(team_id);
CREATE INDEX idx_players_external_id ON players(external_id);

CREATE TABLE matches (
    id                  BIGSERIAL       PRIMARY KEY,
    game_id             BIGINT          NOT NULL REFERENCES games(id),
    team_a_id           BIGINT          NOT NULL REFERENCES teams(id),
    team_b_id           BIGINT          NOT NULL REFERENCES teams(id),
    tournament_name     VARCHAR(200)    NOT NULL,
    stage               VARCHAR(100),
    scheduled_at        TIMESTAMPTZ     NOT NULL,
    -- 경기 상태: SCHEDULED(예정) / ONGOING(진행중) / COMPLETED(완료) / CANCELLED(취소)
    status              VARCHAR(20)     NOT NULL CHECK (status IN ('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    external_id         VARCHAR(100)    UNIQUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- 같은 팀끼리 경기 불가
    CONSTRAINT chk_teams_different CHECK (team_a_id != team_b_id)
);

CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX idx_matches_game_id ON matches(game_id);
CREATE INDEX idx_matches_scheduled_at ON matches(scheduled_at);
CREATE INDEX idx_matches_external_id ON matches(external_id);

CREATE TABLE match_results (
    id              BIGSERIAL       PRIMARY KEY,
    -- 경기당 결과는 하나만 존재 (UNIQUE 제약)
    match_id        BIGINT          NOT NULL UNIQUE REFERENCES matches(id),
    winner_team_id  BIGINT          NOT NULL REFERENCES teams(id),
    score_team_a    INT             NOT NULL DEFAULT 0,
    score_team_b    INT             NOT NULL DEFAULT 0,
    played_at       TIMESTAMPTZ     NOT NULL,
    vod_url         TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 팀별 우승 횟수 집계 쿼리 성능을 위한 인덱스
CREATE INDEX idx_match_results_winner_team_id ON match_results(winner_team_id);
