CREATE TABLE llm_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id           UUID REFERENCES chats(id) ON DELETE SET NULL,
    message_id        UUID REFERENCES messages(id) ON DELETE SET NULL,
    request_type      VARCHAR(20) NOT NULL,
    system_prompt     TEXT NOT NULL,
    user_message      TEXT NOT NULL,
    response_content  TEXT,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    total_tokens      INTEGER,
    duration_ms       BIGINT,
    error             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_llm_logs_type CHECK (
        request_type IN ('guard', 'extract', 'format', 'title')
    )
);

CREATE INDEX idx_llm_logs_chat_id ON llm_logs(chat_id);
CREATE INDEX idx_llm_logs_created_at ON llm_logs(created_at DESC);
