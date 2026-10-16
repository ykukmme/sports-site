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
-- 경기 삭제 시 SET NULL 처리용
CREATE INDEX idx_bracket_nodes_match_id ON bracket_nodes (match_id) WHERE match_id IS NOT NULL;

-- 팀 줄 표시 문구 — 어드민 입력만 (예: 레전드 1위). 진출 관계는 bracket_edges 가 담당
CREATE TABLE bracket_slots (
    id BIGSERIAL PRIMARY KEY,
    node_id BIGINT NOT NULL REFERENCES bracket_nodes(id) ON DELETE CASCADE,
    slot_index SMALLINT NOT NULL CHECK (slot_index IN (0, 1)),
    label VARCHAR(100) NOT NULL CHECK (length(trim(label)) > 0),
    UNIQUE (node_id, slot_index)
);

-- 진출 간선 — source 경기의 승자/패자가 target 경기로 진출 (설계 §14.3)
CREATE TABLE bracket_edges (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES competition_stages(id) ON DELETE CASCADE,
    source_node_id BIGINT NOT NULL REFERENCES bracket_nodes(id) ON DELETE CASCADE,
    target_node_id BIGINT NOT NULL REFERENCES bracket_nodes(id) ON DELETE CASCADE,
    outcome VARCHAR(10) NOT NULL CHECK (outcome IN ('WINNER', 'LOSER')),
    -- AUTO: 동기화가 채움 / ADMIN: 어드민 보정 (재동기화로 덮어쓰지 않음)
    origin VARCHAR(10) NOT NULL CHECK (origin IN ('AUTO', 'ADMIN')),
    -- 대상 경기의 팀 줄 (0/1). 근거가 없으면 NULL — 자동 수집은 항상 NULL
    target_slot_index SMALLINT CHECK (target_slot_index IS NULL OR target_slot_index IN (0, 1)),
    CHECK (source_node_id <> target_node_id),
    -- 한 경기의 승자(또는 패자)는 한 곳으로만 진출
    UNIQUE (source_node_id, outcome)
);

CREATE INDEX idx_bracket_edges_stage_id ON bracket_edges (stage_id);
CREATE INDEX idx_bracket_edges_target_node_id ON bracket_edges (target_node_id);
-- 한 팀 줄에는 확정 간선 하나만
CREATE UNIQUE INDEX uq_bracket_edges_target_slot ON bracket_edges (target_node_id, target_slot_index)
    WHERE target_slot_index IS NOT NULL;
