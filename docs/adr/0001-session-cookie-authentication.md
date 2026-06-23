# Use Session-Cookie Authentication

Prompt Vault uses Spring Security session-cookie authentication for the initial login/signup implementation, rather than JWT-based stateless authentication. This keeps authentication simple for the Spring Boot REST API and React frontend while still supporting an OpenAPI-driven JSON login flow; JWTs can be reconsidered later if the application needs stateless or cross-service authentication.
