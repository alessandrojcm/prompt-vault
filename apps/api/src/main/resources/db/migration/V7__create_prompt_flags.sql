CREATE TABLE prompt_flags
(
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    prompt_id  BIGINT    NOT NULL,
    flagged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_flags_prompt_id UNIQUE (prompt_id),
    CONSTRAINT fk_prompt_flags_prompt FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE
);

CREATE TABLE prompt_flag_keyword_snapshots
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    prompt_flag_id BIGINT       NOT NULL,
    keyword_text   VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    INDEX          idx_prompt_flag_keyword_snapshots_prompt_flag_id (prompt_flag_id),
    CONSTRAINT fk_prompt_flag_keyword_snapshots_prompt_flag FOREIGN KEY (prompt_flag_id) REFERENCES prompt_flags (id) ON DELETE CASCADE
);
