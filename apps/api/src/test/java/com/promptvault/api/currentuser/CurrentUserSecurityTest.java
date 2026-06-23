package com.promptvault.api.currentuser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CurrentUserSecurityTest {

    private static final String MYSQL_SERVICE = "mysql-1";
    private static final int MYSQL_PORT = 3306;

    @Container
    static final ComposeContainer environment = new ComposeContainer(resolveComposeFile())
        .withExposedService(
            MYSQL_SERVICE,
            MYSQL_PORT,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3))
        );

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUri;

    CurrentUserSecurityTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://%s:%d/prompt_vault"
            .formatted(environment.getServiceHost(MYSQL_SERVICE, MYSQL_PORT), environment.getServicePort(MYSQL_SERVICE, MYSQL_PORT)));
        registry.add("spring.datasource.username", () -> "prompt_vault");
        registry.add("spring.datasource.password", () -> "prompt_vault");
        registry.add("spring.flyway.default-schema", () -> "prompt_vault");
        registry.add("spring.flyway.schemas", () -> "prompt_vault");
    }

    @Test
    void returnsUnauthorizedWhenNoSessionExists() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/user")).GET().build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(401);
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
