-- PandaScore API 수집 이력 및 검증 거부 로그
-- Hard Rule: external data validation — 거부 사유를 반드시 기록
CREATE TABLE pandascore_sync_log (
    id          BIGSERIAL    PRIMARY KEY,
    resource    VARCHAR(50)  NOT NULL,   -- MATCH / TEAM / PLAYER
    external_id VARCHAR(100),
    status      VARCHAR(20)  NOT NULL,   -- SUCCESS / REJECTED / ERROR
    reason      TEXT,
    synced_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sync_log_status_at ON pandascore_sync_log(status, synced_at);
