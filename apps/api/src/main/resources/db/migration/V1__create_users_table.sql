CREATE TABLE users
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(64)  NOT NULL,
    email_address  VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('USER', 'ADMIN') NOT NULL,
    account_status ENUM('ACTIVE', 'DISABLED') NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email_address UNIQUE (email_address)
);
