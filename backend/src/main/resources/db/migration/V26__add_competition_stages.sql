-- 대회 단계 — PandaScore tournament 1:1. 어드민이 표시 방식(없음/스위스/브래킷)을 선택한다
CREATE TABLE competition_stages (
    id BIGSERIAL PRIMARY KEY,
    -- PandaScore tournament id (단계 식별 키)
    external_tournament_id BIGINT NOT NULL UNIQUE,
    -- 리그명 (예: LCK, Worlds)
    league_name VARCHAR(200) NOT NULL,
    -- 시리즈명 (예: LCK 2026)
    series_name VARCHAR(200),
    -- 단계명 (예: Playoffs, Group Stage)
    stage_name VARCHAR(200) NOT NULL,
    has_bracket BOOLEAN NOT NULL DEFAULT FALSE,
    -- 표시 방식 — 기본 NONE (어드민이 켜야 공개 노출)
    display_type VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (display_type IN ('NONE', 'SWISS', 'BRACKET')),
    -- 스위스 진출 승수 / 탈락 패수 (월즈 기준 3/3)
    swiss_win_target SMALLINT CHECK (swiss_win_target BETWEEN 1 AND 5),
    swiss_loss_target SMALLINT CHECK (swiss_loss_target BETWEEN 1 AND 5),
    bracket_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_competition_stages_league_name ON competition_stages (league_name);
CREATE INDEX idx_competition_stages_display_type ON competition_stages (display_type);
