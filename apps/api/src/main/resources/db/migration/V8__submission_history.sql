CREATE TABLE prompt_submission_history (
    id BIGINT NOT NULL PRIMARY KEY,
    prompt_id UUID NOT NULL REFERENCES prompt(id),
    submission_id UUID NOT NULL REFERENCES submission(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()

    PRIMARY KEY (id);
    INDEX idx_prompt_submission (prompt_id, submission_id);
);