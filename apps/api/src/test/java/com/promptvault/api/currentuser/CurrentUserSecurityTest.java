package com.promptvault.api.currentuser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.promptvault.api.support.AbstractMySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrentUserSecurityTest extends AbstractMySqlIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUri;

    CurrentUserSecurityTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void returnsUnauthorizedWhenNoSessionExists() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/user")).GET().build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
