-- 대진표 노드 — PandaScore bracket 경기 1:1 (미정 TBD 경기 포함)
CREATE TABLE bracket_nodes (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES competition_stages(id) ON DELETE CASCADE,
    -- PandaScore match id
    external_match_id BIGINT NOT NULL UNIQUE,
    -- 확정·가져오기 완료된 경기와 연결 (없으면 NULL)
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    -- 라운드 라벨 (예: Upper bracket semifinal) — 경기 번호 제외
    round_label VARCHAR(100) NOT NULL,
    -- 라운드 내 경기 번호 (예: semifinal 2 의 2)
    sequence_in_round SMALLINT,
    section VARCHAR(10) NOT NULL CHECK (section IN ('UPPER', 'LOWER', 'FINAL')),
    -- 자동 배치 열/행
    auto_column SMALLINT NOT NULL,
    auto_row SMALLINT NOT NULL,
    -- 어드민 보정 (NULL 이면 자동 배치 사용)
    column_override SMALLINT,
    row_override SMALLINT,
    best_of SMALLINT CHECK (best_of IS NULL OR best_of IN (1, 3, 5, 7)),
    scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_bracket_nodes_stage_id ON bracket_nodes (stage_id);

-- 노드 슬롯 — 노드당 2개(0, 1). 출처: 미상 / 선행 경기 승자 / 선행 경기 패자 / 어드민 라벨
CREATE TABLE bracket_slots (
    id BIGSERIAL PRIMARY KEY,
    node_id BIGINT NOT NULL REFERENCES bracket_nodes(id) ON DELETE CASCADE,
    slot_index SMALLINT NOT NULL CHECK (slot_index IN (0, 1)),
    source_type VARCHAR(20) NOT NULL
        CHECK (source_type IN ('UNKNOWN', 'WINNER_OF', 'LOSER_OF', 'LABEL')),
    source_node_id BIGINT REFERENCES bracket_nodes(id) ON DELETE SET NULL,
    -- 어드민 입력 문구 (예: 레전드 1위)
    label VARCHAR(100),
    -- AUTO: 동기화가 채움 / ADMIN: 어드민 보정 (재동기화로 덮어쓰지 않음)
    origin VARCHAR(10) NOT NULL CHECK (origin IN ('AUTO', 'ADMIN')),
    UNIQUE (node_id, slot_index),
    CHECK (source_type NOT IN ('WINNER_OF', 'LOSER_OF') OR source_node_id IS NOT NULL),
    CHECK (source_type <> 'LABEL' OR label IS NOT NULL)
);
