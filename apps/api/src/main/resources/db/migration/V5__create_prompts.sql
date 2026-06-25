CREATE TABLE prompts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    prompt_text TEXT NOT NULL,
    visibility ENUM('PRIVATE', 'PUBLIC') NOT NULL DEFAULT 'PRIVATE',
    owner_user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_prompts_owner_user_id_created_at (owner_user_id, created_at, id),
    INDEX idx_prompts_category_id (category_id),
    CONSTRAINT fk_prompts_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_prompts_category FOREIGN KEY (category_id) REFERENCES prompt_categories (id)
);
