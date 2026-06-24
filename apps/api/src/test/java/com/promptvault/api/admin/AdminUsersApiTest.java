package com.promptvault.api.admin;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
class AdminUsersApiTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI baseUri;

    AdminUsersApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void adminsCanListNormalUsersWithSafeManagementFields() throws Exception {
        String adminPassword = "admin-password123";
        UserEntity admin = saveUser(uniqueUsername("admin"), adminPassword, Role.ADMIN, AccountStatus.ENABLED);
        UserEntity enabledUser = saveUser(uniqueUsername("enabled"), "password123", Role.USER, AccountStatus.ENABLED);
        UserEntity disabledUser = saveUser(uniqueUsername("disabled"), "password123", Role.USER, AccountStatus.DISABLED);
        HttpClient adminClient = authenticatedClient(admin.getUsername(), adminPassword);

        HttpResponse<String> response = listUsers(adminClient, "USER");

        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> users = readList(response.body());
        assertThat(users).anySatisfy(user -> assertUserSummary(user, enabledUser, Role.USER, AccountStatus.ENABLED));
        assertThat(users).anySatisfy(user -> assertUserSummary(user, disabledUser, Role.USER, AccountStatus.DISABLED));
        assertThat(users).noneSatisfy(user -> assertThat(user).containsEntry("username", admin.getUsername()));
        assertThat(users).allSatisfy(user -> assertThat(user).doesNotContainKeys("password", "passwordHash"));
    }

    @Test
    void omittingRoleFilterReturnsAllRegisteredUsers() throws Exception {
        String adminPassword = "admin-password123";
        UserEntity admin = saveUser(uniqueUsername("allAdmin"), adminPassword, Role.ADMIN, AccountStatus.ENABLED);
        UserEntity normalUser = saveUser(uniqueUsername("allUser"), "password123", Role.USER, AccountStatus.ENABLED);
        HttpClient adminClient = authenticatedClient(admin.getUsername(), adminPassword);

        HttpResponse<String> response = listUsers(adminClient, null);

        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> users = readList(response.body());
        assertThat(users).anySatisfy(user -> assertUserSummary(user, admin, Role.ADMIN, AccountStatus.ENABLED));
        assertThat(users).anySatisfy(user -> assertUserSummary(user, normalUser, Role.USER, AccountStatus.ENABLED));
    }

    @Test
    void invalidRoleFilterReturnsValidationErrorShape() throws Exception {
        String adminPassword = "admin-password123";
        UserEntity admin = saveUser(uniqueUsername("invalidRoleAdmin"), adminPassword, Role.ADMIN, AccountStatus.ENABLED);
        HttpClient adminClient = authenticatedClient(admin.getUsername(), adminPassword);

        HttpResponse<String> response = listUsers(adminClient, "OWNER");

        assertThat(response.statusCode()).isEqualTo(400);
        Map<String, Object> body = readJson(response.body());
        assertThat(body).containsEntry("message", "Signup validation failed.");
        assertThat(extractFieldMessages(body)).containsEntry("role", "Role must be USER or ADMIN.");
    }

    @Test
    void unauthenticatedCallersCannotListUsers() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        HttpResponse<String> response = listUsers(client, "USER");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void normalUsersAreForbiddenFromListingUsers() throws Exception {
        String password = "password123";
        UserEntity user = saveUser(uniqueUsername("normalForbidden"), password, Role.USER, AccountStatus.ENABLED);
        HttpClient userClient = authenticatedClient(user.getUsername(), password);

        HttpResponse<String> response = listUsers(userClient, "USER");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private HttpClient authenticatedClient(String username, String password) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> loginResponse = login(username, password, client);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return client;
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

    private HttpResponse<String> listUsers(HttpClient client, String role) throws Exception {
        URI uri = role == null
            ? baseUri.resolve("/api/admin/users")
            : baseUri.resolve("/api/admin/users?role=" + role);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private UserEntity saveUser(String username, String password, Role role, AccountStatus accountStatus) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(username.toLowerCase(java.util.Locale.ROOT));
        user.setEmailAddress(username + "@example.com");
        user.setEmailAddressNormalized(username.toLowerCase(java.util.Locale.ROOT) + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountStatus(accountStatus);
        return userRepository.save(user);
    }

    private void assertUserSummary(Map<String, Object> user, UserEntity expected, Role role, AccountStatus accountStatus) {
        assertThat(user).containsEntry("id", expected.getId().intValue());
        assertThat(user).containsEntry("username", expected.getUsername());
        assertThat(user).containsEntry("emailAddress", expected.getEmailAddress());
        assertThat(user).containsEntry("role", role.name());
        assertThat(user).containsEntry("accountStatus", accountStatus.name());
    }

    private List<Map<String, Object>> readList(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    private Map<String, Object> readJson(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractFieldMessages(Map<String, Object> body) {
        return ((List<Map<String, String>>) body.get("fieldErrors"))
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                fieldError -> fieldError.get("field"),
                fieldError -> fieldError.get("message")
            ));
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
