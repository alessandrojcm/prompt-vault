INSERT INTO users (
    username,
    username_normalized,
    email_address,
    email_address_normalized,
    password_hash,
    role,
    account_status
) VALUES (
    'admin',
    'admin',
    'admin@promptvault.local',
    'admin@promptvault.local',
    '{bcrypt}$2a$10$bbzFzEunHjXRM2AQx3.3QeeBMdDT6vKrrfE0Yv7FpC18JNpDutZ8W',
    'ADMIN',
    'ENABLED'
);
