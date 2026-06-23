ALTER TABLE users
    DROP INDEX uk_users_username,
    DROP INDEX uk_users_email_address,
    MODIFY username VARCHAR(30) NOT NULL,
    ADD COLUMN username_normalized VARCHAR(30) NOT NULL AFTER username,
    ADD COLUMN email_address_normalized VARCHAR(255) NOT NULL AFTER email_address,
    MODIFY account_status ENUM('ENABLED', 'DISABLED') NOT NULL,
    ADD CONSTRAINT uk_users_username_normalized UNIQUE (username_normalized),
    ADD CONSTRAINT uk_users_email_address_normalized UNIQUE (email_address_normalized);
