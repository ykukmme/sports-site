-- 경기 BO(Best-of) 수 — PandaScore match_type = best_of 일 때만 채운다. 그 외/미상은 NULL
ALTER TABLE matches ADD COLUMN best_of SMALLINT;
ALTER TABLE matches ADD CONSTRAINT chk_matches_best_of
    CHECK (best_of IS NULL OR best_of IN (1, 3, 5, 7));

-- PandaScore tournament id — 대회 단계 식별 키 (수동 등록 경기는 NULL)
-- 기존 PandaScore 경기는 다음 가져오기/결과 동기화 때 자연 채움 (V21 과 같은 정책)
ALTER TABLE matches ADD COLUMN external_tournament_id BIGINT;
CREATE INDEX idx_matches_external_tournament_id ON matches (external_tournament_id);
