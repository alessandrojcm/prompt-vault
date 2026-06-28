CREATE TABLE prompt_categories
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    label              VARCHAR(100) NOT NULL,
    label_normalized   VARCHAR(100) NOT NULL,
    slug               VARCHAR(100) NOT NULL,
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
                               created_by_user_id)
SELECT seed.label, LOWER(seed.label), seed.slug, admin.id
FROM (SELECT 'Coding' AS label, 'coding' AS slug
      UNION ALL
      SELECT 'Research', 'research'
      UNION ALL
      SELECT 'Cybersecurity', 'cybersecurity'
      UNION ALL
      SELECT 'HR', 'hr'
      UNION ALL
      SELECT 'Legal', 'legal'
      UNION ALL
      SELECT 'Personal Productivity', 'personal_productivity') seed
         CROSS JOIN users admin
WHERE admin.username_normalized = 'admin'
  AND admin.role = 'ADMIN';
