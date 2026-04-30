-- 자동/수동 바인딩 audit 컬럼 추가 (legacy row는 NULL)
ALTER TABLE match_external_detail
    ADD COLUMN bound_by    VARCHAR(16),
    ADD COLUMN bound_at    TIMESTAMPTZ,
    ADD COLUMN bound_score INT;

ALTER TABLE match_external_detail
    ADD CONSTRAINT match_external_detail_bound_by_check
        CHECK (bound_by IS NULL OR bound_by IN ('AUTO', 'MANUAL'));
