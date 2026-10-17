-- 단계 원본 경기 스냅샷 — PandaScore tournament 경기 목록 전체 (TBD·취소 포함, 설계 §14.4.1)
-- 스위스 데이터 완전성 판정용. 동기화마다 단계 단위로 전체 교체한다
CREATE TABLE stage_source_matches (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES competition_stages(id) ON DELETE CASCADE,
    -- PandaScore match id
    external_match_id BIGINT NOT NULL,
    scheduled_at TIMESTAMPTZ,
    -- PandaScore 원본 status
    source_status VARCHAR(20) NOT NULL
        CHECK (source_status IN ('not_started', 'running', 'finished', 'canceled', 'postponed')),
    -- 양 팀 PandaScore team id / 이름 (TBD 면 NULL)
    team_a_external_id BIGINT,
    team_a_name VARCHAR(200),
    team_b_external_id BIGINT,
    team_b_name VARCHAR(200),
    -- 종료 경기의 승자 PandaScore team id
    winner_external_id BIGINT,
    -- 완전성 판정 제외 사유 (NULL 이면 판정 대상)
    exclusion_reason VARCHAR(20) CHECK (exclusion_reason IS NULL OR exclusion_reason IN ('TBD', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (stage_id, external_match_id),
    -- 종료 경기는 승자가 있어야 한다 (Hard Rule #8 — 검증 통과 데이터만 저장)
    CHECK (source_status <> 'finished' OR winner_external_id IS NOT NULL)
);

-- 마지막 원본 스냅샷 시각 — NULL 이면 완전성 UNKNOWN
ALTER TABLE competition_stages ADD COLUMN source_synced_at TIMESTAMPTZ;
