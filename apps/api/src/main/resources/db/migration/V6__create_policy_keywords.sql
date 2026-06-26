CREATE TABLE policy_keywords (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(100) NOT NULL,
    keyword_normalized VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_policy_keywords_keyword_normalized UNIQUE (keyword_normalized),
    CONSTRAINT fk_policy_keywords_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);
