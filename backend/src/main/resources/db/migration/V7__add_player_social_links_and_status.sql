ALTER TABLE players
    ADD COLUMN instagram_url VARCHAR(255),
    ADD COLUMN x_url VARCHAR(255),
    ADD COLUMN youtube_url VARCHAR(255),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN birth_date DATE,
    ADD COLUMN external_source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN last_synced_at TIMESTAMPTZ;

ALTER TABLE players
    ADD CONSTRAINT chk_players_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'RETIRED'));

ALTER TABLE players
    ADD CONSTRAINT chk_players_external_source
        CHECK (external_source IN ('MANUAL', 'PANDASCORE'));
