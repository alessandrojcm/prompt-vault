package com.promptvault.api.currentuser;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.api.support.AbstractMySqlIntegrationTest;
import com.promptvault.api.user.AccountStatus;
import com.promptvault.api.user.Role;
import com.promptvault.api.user.UserEntity;
import com.promptvault.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrentUserSecurityTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
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

    @Test
    void loginEstablishesSessionCookieAndCurrentUserReturnsSafeUserSummary() throws Exception {
        String username = uniqueUsername("loginUser");
        String password = "password123";
        UserEntity user = saveUser(username, password, Role.USER);

        HttpResponse<String> loginResponse = login(username.toUpperCase(), password, httpClient);

        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.headers().allValues("set-cookie"))
            .anySatisfy(cookie -> assertThat(cookie).contains("JSESSIONID"));
        assertSafeUserSummary(loginResponse.body(), user, Role.USER);

        HttpResponse<String> currentUserResponse = getCurrentUser(httpClient);

        assertThat(currentUserResponse.statusCode()).isEqualTo(200);
        assertSafeUserSummary(currentUserResponse.body(), user, Role.USER);
    }

    @Test
    void invalidCredentialsReturnUnauthorizedWithGenericMessageAndNoSessionCookie() throws Exception {
        String username = uniqueUsername("badLogin");
        saveUser(username, "password123", Role.USER);

        HttpResponse<String> response = login(username, "wrong-password", httpClient);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(readJson(response.body())).containsEntry("message", "Invalid username or password.");
        assertThat(response.headers().allValues("set-cookie")).noneSatisfy(cookie -> assertThat(cookie).contains("JSESSIONID"));
    }

    @Test
    void adminsAndNormalUsersUseTheSameLoginAndCurrentUserFlow() throws Exception {
        String userPassword = "user-password123";
        String adminPassword = "admin-password123";
        UserEntity normalUser = saveUser(uniqueUsername("normal"), userPassword, Role.USER);
        UserEntity adminUser = saveUser(uniqueUsername("admin"), adminPassword, Role.ADMIN);

        HttpClient normalUserClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> normalLoginResponse = login(normalUser.getUsername(), userPassword, normalUserClient);
        HttpResponse<String> normalCurrentUserResponse = getCurrentUser(normalUserClient);

        assertThat(normalLoginResponse.statusCode()).isEqualTo(200);
        assertThat(normalCurrentUserResponse.statusCode()).isEqualTo(200);
        assertSafeUserSummary(normalCurrentUserResponse.body(), normalUser, Role.USER);

        HttpClient adminClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> adminLoginResponse = login(adminUser.getUsername(), adminPassword, adminClient);
        HttpResponse<String> adminCurrentUserResponse = getCurrentUser(adminClient);

        assertThat(adminLoginResponse.statusCode()).isEqualTo(200);
        assertThat(adminCurrentUserResponse.statusCode()).isEqualTo(200);
        assertSafeUserSummary(adminCurrentUserResponse.body(), adminUser, Role.ADMIN);
    }

    @Test
    void logoutRequiresAnAuthenticatedSession() throws Exception {
        HttpResponse<Void> response = logout(httpClient);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void logoutInvalidatesTheSessionAndCurrentUserBecomesUnauthorized() throws Exception {
        String username = uniqueUsername("logoutUser");
        String password = "password123";
        saveUser(username, password, Role.USER);

        HttpResponse<String> loginResponse = login(username, password, httpClient);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(getCurrentUser(httpClient).statusCode()).isEqualTo(200);

        HttpResponse<Void> logoutResponse = logout(httpClient);

        assertThat(logoutResponse.statusCode()).isEqualTo(204);
        assertThat(getCurrentUser(httpClient).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> login(String username, String password, HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/login"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
            ))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getCurrentUser(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/user"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<Void> logout(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/logout"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private UserEntity saveUser(String username, String password, Role role) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(username.toLowerCase(java.util.Locale.ROOT));
        user.setEmailAddress(username + "@example.com");
        user.setEmailAddressNormalized(username.toLowerCase(java.util.Locale.ROOT) + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ENABLED);
        return userRepository.save(user);
    }

    private void assertSafeUserSummary(String body, UserEntity user, Role role) throws Exception {
        Map<String, Object> responseBody = readJson(body);
        assertThat(responseBody).containsEntry("id", user.getId().intValue());
        assertThat(responseBody).containsEntry("username", user.getUsername());
        assertThat(responseBody).containsEntry("emailAddress", user.getEmailAddress());
        assertThat(responseBody).containsEntry("role", role.name());
        assertThat(responseBody).containsEntry("accountStatus", "ENABLED");
        assertThat(responseBody).doesNotContainKeys("password", "passwordHash", "passwordUpdatedAt");
    }

    private Map<String, Object> readJson(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
