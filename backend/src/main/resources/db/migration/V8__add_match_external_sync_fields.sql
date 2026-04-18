ALTER TABLE matches
    ADD COLUMN external_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN last_synced_at TIMESTAMPTZ;

CREATE INDEX idx_matches_external_source ON matches(external_source);
