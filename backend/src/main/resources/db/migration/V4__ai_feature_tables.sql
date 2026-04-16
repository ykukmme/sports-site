-- AI 하이라이트 요약 처리 큐
CREATE TABLE match_summary_queue (
    id          BIGSERIAL    PRIMARY KEY,
    match_id    BIGINT       NOT NULL UNIQUE REFERENCES matches(id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING: 대기 / PROCESSING: 처리 중 / DONE: 완료 / FAILED: 실패
    retry_count INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_summary_queue_status ON match_summary_queue(status, created_at);

-- 생성된 AI 하이라이트 요약 저장
CREATE TABLE match_ai_summaries (
    id                BIGSERIAL    PRIMARY KEY,
    match_id          BIGINT       NOT NULL UNIQUE REFERENCES matches(id),
    summary_text      TEXT         NOT NULL,
    model_version     VARCHAR(100) NOT NULL,
    prompt_tokens     INT          NOT NULL,
    completion_tokens INT          NOT NULL,
    generated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- AI API 호출 비용 추적 (Hard Rule: AI cost cap)
CREATE TABLE ai_usage_log (
    id                  BIGSERIAL     PRIMARY KEY,
    feature             VARCHAR(20)   NOT NULL,  -- SUMMARY / CHATBOT
    prompt_tokens       INT           NOT NULL,
    completion_tokens   INT           NOT NULL,
    estimated_cost_usd  DECIMAL(10,6) NOT NULL,
    used_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_usage_log_used_at ON ai_usage_log(used_at);
