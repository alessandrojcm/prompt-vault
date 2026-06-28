CREATE TABLE prompt_categories
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    label              VARCHAR(100) NOT NULL,
    label_normalized   VARCHAR(100) NOT NULL,
    slug               VARCHAR(100) NOT NULL,
    description        VARCHAR(255),
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT       NOT NULL,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_prompt_categories_label_normalized UNIQUE (label_normalized),
    CONSTRAINT uk_prompt_categories_slug UNIQUE (slug),
    CONSTRAINT fk_prompt_categories_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

INSERT INTO prompt_categories (label,
                                label_normalized,
                                slug,
                                description,
                                created_by_user_id)
SELECT seed.label, LOWER(seed.label), seed.slug, seed.description, admin.id
FROM (SELECT 'Coding' AS label, 'coding' AS slug, 'Prompts for software development and code-related tasks.' AS description
      UNION ALL
      SELECT 'Research', 'research', 'Prompts for investigation, analysis, and synthesis.'
      UNION ALL
      SELECT 'Cybersecurity', 'cybersecurity', 'Prompts for security analysis and defensive workflows.'
      UNION ALL
      SELECT 'HR', 'hr', 'Prompts for human resources and people operations.'
      UNION ALL
      SELECT 'Legal', 'legal', 'Prompts for legal drafting and review support.'
      UNION ALL
      SELECT 'Personal Productivity', 'personal_productivity', 'Prompts for planning, organization, and focus.') seed
         CROSS JOIN users admin
WHERE admin.username_normalized = 'admin'
  AND admin.role = 'ADMIN';
