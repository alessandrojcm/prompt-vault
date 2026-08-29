package com.promptvault.api.config;

import com.promptvault.api.support.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "prompt-vault.security.csrf-enabled=false")
@ActiveProfiles("prod")
class ProdHardeningTest extends AbstractMySqlIntegrationTest {

    private final URI baseUri;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Environment environment;

    ProdHardeningTest(@Value("${local.server.port}") int port, @Autowired Environment environment) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
        this.environment = environment;
    }

    @Test
    void sessionCookieIsFlaggedSecureInProdProfile() throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder(baseUri.resolve("/api/login"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"admin\",\"password\":\"admin-password123\"}"))
                .build();
        HttpResponse<String> response = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("set-cookie"))
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains("JSESSIONID");
                    assertThat(cookie).contains("Secure");
                });
    }

    @Test
    void springdocEndpointsAreNotPubliclyServedInProdProfile() throws Exception {
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");

        HttpResponse<Void> docs = httpClient.send(HttpRequest.newBuilder(baseUri.resolve("/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        HttpResponse<Void> swagger = httpClient.send(
                HttpRequest.newBuilder(baseUri.resolve("/swagger-ui/index.html")).GET().build(),
                HttpResponse.BodyHandlers.discarding());

        // With springdoc disabled the routes no longer exist, so they fall through to the
        // authenticated catch-all: unauthenticated callers are denied (401/404, never 200
        // with an OpenAPI document).
        assertThat(docs.statusCode()).isNotEqualTo(200);
        assertThat(swagger.statusCode()).isNotEqualTo(200);
    }

    @Test
    void cspHeaderIsPresentOnApiResponsesInProdProfile() throws Exception {
        HttpResponse<Void> response = httpClient.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/user")).GET().build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
    }
}