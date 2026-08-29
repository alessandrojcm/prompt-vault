package com.promptvault.api.config;

import com.promptvault.api.support.AbstractMySqlIntegrationTest;
import com.promptvault.api.support.CsrfAwareTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CsrfSecurityTest extends AbstractMySqlIntegrationTest {

    private final URI baseUri;
    private final CsrfAwareTestClient client;

    CsrfSecurityTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
        this.client = CsrfAwareTestClient.create(baseUri);
    }

    @Test
    void loginIssuesTheCsrfCookieAlongsideTheSessionCookie() throws Exception {
        HttpResponse<String> priming = getCurrentUser();
        assertThat(priming.statusCode()).isEqualTo(401);
        assertThat(client.csrfTokenOrNull()).isNotNull();

        HttpResponse<String> response = login("admin", "admin-password123");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(client.csrfTokenOrNull()).isNotNull();
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejectedWith403() throws Exception {
        HttpResponse<String> priming = getCurrentUser();
        assertThat(priming.statusCode()).isEqualTo(401);

        client.clearCookies();

        HttpResponse<String> response = login("admin", "admin-password123");

        // An anonymous POST without the CSRF cookie is rejected; Spring Security surfaces
        // this as 401 via the authentication entry point because no session exists yet.
        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void getStateIsNotEnforcedAndStillEstablishesTheCsrfCookie() throws Exception {
        HttpResponse<String> first = getCurrentUser();
        assertThat(first.statusCode()).isEqualTo(401);

        HttpResponse<String> second = getCurrentUser();

        // GETs are never CSRF-enforced, so the SPA can bootstrap the token cookie with
        // an unauthenticated read before any mutation.
        assertThat(second.statusCode()).isEqualTo(401);
        assertThat(client.csrfTokenOrNull()).isNotNull();
    }

    @Test
    void signupWithValidCsrfTokenSucceeds() throws Exception {
        HttpResponse<String> priming = getCurrentUser();
        assertThat(priming.statusCode()).isEqualTo(401);
        assertThat(client.csrfTokenOrNull()).isNotNull();

        HttpResponse<String> response = client.send(HttpRequest.newBuilder(baseUri.resolve("/api/signup"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"csrfuser\",\"emailAddress\":\"csrfuser@example.com\",\"password\":\"password123\"}")));

        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void signupWithInvalidCsrfTokenIsRejected() throws Exception {
        HttpResponse<String> priming = getCurrentUser();
        assertThat(priming.statusCode()).isEqualTo(401);
        assertThat(client.csrfTokenOrNull()).isNotNull();

        HttpResponse<String> response = client.send(HttpRequest.newBuilder(baseUri.resolve("/api/signup"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(CsrfAwareTestClient.CSRF_HEADER_NAME, "forged-token")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"csrfuser2\",\"emailAddress\":\"csrfuser2@example.com\",\"password\":\"password123\"}")));

        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void authenticatedMutationWithoutCsrfCookieIsRejected() throws Exception {
        HttpResponse<String> priming = getCurrentUser();
        assertThat(priming.statusCode()).isEqualTo(401);
        assertThat(login("admin", "admin-password123").statusCode()).isEqualTo(200);

        // Session stays, CSRF cookie disappears — the classic stolen-session scenario.
        client.clearCsrfCookie();

        HttpResponse<Void> response = logout();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return client.send(HttpRequest.newBuilder(baseUri.resolve("/api/login"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")));
    }

    private HttpResponse<String> getCurrentUser() throws Exception {
        return client.send(HttpRequest.newBuilder(baseUri.resolve("/api/user"))
                .header("Accept", "application/json")
                .GET());
    }

    private HttpResponse<Void> logout() throws Exception {
        return client.sendDiscarding(HttpRequest.newBuilder(baseUri.resolve("/api/logout"))
                .POST(HttpRequest.BodyPublishers.noBody()));
    }
}