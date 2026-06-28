-- Local/dev seeded administrator credentials:
-- username: admin
-- password: admin-password123
INSERT INTO users (username,
                   username_normalized,
                   email_address,
                   email_address_normalized,
                   password_hash,
                   role,
                   account_status)
VALUES ('admin',
        'admin',
        'admin@promptvault.local',
        'admin@promptvault.local',
        '{bcrypt}$2y$10$dTvriZjxawrRb7nIezibcuHzs8oPb5vpnrBJYvTkBp7nv8Cwz7Z9u',
        'ADMIN',
        'ENABLED');
