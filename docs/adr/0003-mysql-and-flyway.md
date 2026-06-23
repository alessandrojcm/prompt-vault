# Use MySQL with Flyway Migrations

Prompt Vault uses MySQL as the application database, with local development provided through Docker Compose. Flyway owns schema migrations and local seed data, including predefined admin users, so database structure and required baseline data are explicit rather than inferred from application startup.
