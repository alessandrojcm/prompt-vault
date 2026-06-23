# Use Spring Data JPA for Persistence Mapping

Prompt Vault uses Spring Data JPA for persistence mapping even though the initial login/signup slice has a simple user model. Upcoming features around prompts, sharing, ownership, and security alerts are expected to need ORM relationships, so starting with JPA avoids an early persistence rewrite while Flyway remains responsible for database schema changes.
