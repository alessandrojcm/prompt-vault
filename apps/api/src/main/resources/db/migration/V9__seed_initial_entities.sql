-- Local/dev seeded administrator credentials:
-- username: user1/user2
-- password: password123
INSERT INTO users (username,
                   username_normalized,
                   email_address,
                   email_address_normalized,
                   password_hash,
                   role,
                   account_status)
VALUES ('user1',
        'user1',
        'user1@user.com',
        'user1@user.com',
        '{bcrypt}$2a$12$xAwwqxN3A05CtjJcsBwZOuG72HWHVXeq/uu4isTXq8IcOgO9yfPAO',
        'USER',
        'ENABLED'),
       ('user2',
        'user2',
        'user2@user.com',
        'user2@user.com',
        '{bcrypt}$2a$12$xAwwqxN3A05CtjJcsBwZOuG72HWHVXeq/uu4isTXq8IcOgO9yfPAO',
        'USER',
        'ENABLED');


INSERT INTO policy_keywords (keyword, keyword_normalized, created_by_user_id)
SELECT seed.keyword, LOWER(seed.keyword_normalized), admin.id
FROM (SELECT 'Secret' AS keyword, 'secret' AS keyword_normalized
      UNION ALL
      SELECT 'API Key' AS keyword, 'api key' AS keyword_normalized
      UNION ALL
      SELECT 'Password' AS keyword, 'password' AS keyword_normalized
      UNION ALL
      SELECT 'Email' AS keyword, 'email' AS keyword_normalized
      UNION ALL
      SELECT 'Credit' AS keyword, 'credit' AS keyword_normalized) seed
         CROSS JOIN users admin
WHERE admin.username_normalized = 'admin'
  AND admin.role = 'ADMIN';


INSERT INTO prompts (title,
                     prompt_text,
                     visibility,
                     owner_user_id,
                     category_id)
SELECT seed.title,
       seed.prompt_text,
       seed.visibility,
       owner.id,
       category.id
FROM (SELECT 'API Refactor Checklist' AS title,
             'Review this service for safe refactoring steps, including how to remove hard-coded API Key and Secret values before release.' AS prompt_text,
             'PUBLIC' AS visibility,
             'admin' AS owner_username_normalized,
             'coding' AS category_slug
      UNION ALL
      SELECT 'Legal Risk Summary',
             'Summarize the contract risks and call out clauses that mention password sharing, email retention, or credit obligations.',
             'PRIVATE',
             'admin',
             'legal'
      UNION ALL
      SELECT 'Research Brief Builder',
             'Create a concise research brief with assumptions, open questions, and suggested sources for the topic below.',
             'PUBLIC',
             'user1',
             'research'
      UNION ALL
      SELECT 'Incident Triage Notes',
             'Turn these incident notes into a timeline and highlight any exposed secret, API Key, password, or email address that needs rotation.',
             'PRIVATE',
             'user1',
             'cybersecurity'
      UNION ALL
      SELECT 'Interview Feedback Formatter',
             'Rewrite this interview feedback into a structured HR summary with strengths, concerns, and follow-up questions.',
             'PUBLIC',
             'user2',
             'hr'
      UNION ALL
      SELECT 'Weekly Focus Planner',
             'Plan my week around three outcomes, estimate effort, and flag any task that requires a credit card or account password.',
             'PRIVATE',
             'user2',
             'personal_productivity') seed
         JOIN users owner ON owner.username_normalized = seed.owner_username_normalized
         JOIN prompt_categories category ON category.slug = seed.category_slug;
