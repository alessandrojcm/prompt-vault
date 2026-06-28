CREATE TABLE prompt_submission_history
(
    id           BIGINT    NOT NULL PRIMARY KEY AUTO_INCREMENT,
    prompt_id    BIGINT    NOT NULL REFERENCES prompts (id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    llm_response TEXT      NOT NULL,

    INDEX idx_prompt_submission (prompt_id, id),
    CONSTRAINT unique_prompt_submission UNIQUE (prompt_id, id)
);