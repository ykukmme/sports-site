-- 게임 단위 어드민 보정 테이블. GOL.GG enrichment가 채운 base 값에 대한 사람 보정값을 저장한다.
-- 머지는 필드 단위 coalesce(override 값, base 값) 규칙으로 적용된다.
-- enrichment 재실행 시 base 만 갱신되며 본 테이블은 손대지 않는다 → 재동기화 안전.
CREATE TABLE match_external_detail_game_override (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL UNIQUE REFERENCES match_external_detail_game(id) ON DELETE CASCADE,
    duration_sec INT NULL CHECK (duration_sec IS NULL OR duration_sec >= 0),
    blue_team_gold INT NULL CHECK (blue_team_gold IS NULL OR blue_team_gold >= 0),
    red_team_gold INT NULL CHECK (red_team_gold IS NULL OR red_team_gold >= 0),
    -- 골드/데미지 분배 JSONB. NULL = base 사용, 값 = override 적용.
    -- 형식: [{"side":"BLUE","position":"TOP","percent":21.5,"perMinute":420.0}, ...]
    gold_distribution_json JSONB NULL,
    damage_distribution_json JSONB NULL,
    note VARCHAR(500) NULL,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_game_override_updated_at ON match_external_detail_game_override(updated_at DESC);

-- 보정 변경 이력. append-only — 어떤 필드가 누구에 의해 어떻게 바뀌었는지 추적.
-- game_id 는 FK 미설정 (원본 게임 삭제 후에도 이력 보존 목적).
CREATE TABLE match_external_detail_game_override_audit (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    field_name VARCHAR(50) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    note VARCHAR(500) NULL,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_game_override_audit_game_id ON match_external_detail_game_override_audit(game_id, updated_at DESC);
