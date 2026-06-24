package com.promptvault.api.support;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractMySqlIntegrationTest {

    protected static final String MYSQL_SERVICE = "mysql-1";
    protected static final int MYSQL_PORT = 3306;

    @Container
    static final ComposeContainer environment = new ComposeContainer(resolveComposeFile())
        .withExposedService(
            MYSQL_SERVICE,
            MYSQL_PORT,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3))
        );

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://%s:%d/prompt_vault"
            .formatted(environment.getServiceHost(MYSQL_SERVICE, MYSQL_PORT), environment.getServicePort(MYSQL_SERVICE, MYSQL_PORT)));
        registry.add("spring.datasource.username", () -> "prompt_vault");
        registry.add("spring.datasource.password", () -> "prompt_vault");
        registry.add("spring.flyway.default-schema", () -> "prompt_vault");
        registry.add("spring.flyway.schemas", () -> "prompt_vault");
    }

    private static File resolveComposeFile() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();

        while (current != null) {
            Path candidate = current.resolve("docker-compose.yml");

            if (candidate.toFile().isFile()) {
                return candidate.toFile();
            }

            current = current.getParent();
        }

        throw new IllegalStateException("Unable to locate docker-compose.yml for Testcontainers");
    }
}
