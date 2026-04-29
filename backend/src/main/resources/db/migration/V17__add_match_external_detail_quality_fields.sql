ALTER TABLE match_external_detail
    DROP CONSTRAINT IF EXISTS match_external_detail_status_check;

ALTER TABLE match_external_detail
    ADD CONSTRAINT match_external_detail_status_check
        CHECK (status IN ('PENDING', 'SYNCED', 'PARTIAL_SYNC', 'FAILED', 'NEEDS_REVIEW'));

ALTER TABLE match_external_detail
    ADD COLUMN expected_game_count INT,
    ADD COLUMN synced_game_count INT,
    ADD COLUMN validation_status VARCHAR(30),
    ADD COLUMN validation_message TEXT,
    ADD COLUMN team_match_confidence INT,
    ADD COLUMN date_match_confidence INT;
