package com.promptvault.api.signup;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

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
class SignupApiTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUri;
    private final ObjectMapper objectMapper = new ObjectMapper();

    SignupApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void signupCreatesAnEnabledNormalUserWithATrimmedIdentityAndHashedPassword() throws Exception {
        String password = "  pass word  ";
        HttpResponse<String> response = signup(Map.of(
            "username", "  Alice.User  ",
            "emailAddress", "  Alice@example.com  ",
            "password", password
        ));

        assertThat(response.statusCode()).isEqualTo(201);

        Map<String, Object> responseBody = readJson(response.body());
        assertThat(responseBody).containsEntry("username", "Alice.User");
        assertThat(responseBody).containsEntry("emailAddress", "Alice@example.com");
        assertThat(responseBody).containsEntry("role", "USER");
        assertThat(responseBody).containsEntry("accountStatus", "ENABLED");
        assertThat(responseBody).doesNotContainKey("password");
        assertThat(responseBody).doesNotContainKey("passwordHash");

        UserEntity persistedUser = userRepository.findByUsernameNormalized("alice.user").orElseThrow();
        assertThat(persistedUser.getUsername()).isEqualTo("Alice.User");
        assertThat(persistedUser.getEmailAddress()).isEqualTo("Alice@example.com");
        assertThat(persistedUser.getUsernameNormalized()).isEqualTo("alice.user");
        assertThat(persistedUser.getEmailAddressNormalized()).isEqualTo("alice@example.com");
        assertThat(persistedUser.getRole()).isEqualTo(Role.USER);
        assertThat(persistedUser.getAccountStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, persistedUser.getPasswordHash())).isTrue();
    }

    @Test
    void signupReturnsAllDuplicateFieldErrorsCaseInsensitively() throws Exception {
        signup(Map.of(
            "username", "Casey",
            "emailAddress", "casey@example.com",
            "password", "password123"
        ));

        HttpResponse<String> response = signup(Map.of(
            "username", "casey",
            "emailAddress", "CASEY@example.com",
            "password", "password456"
        ));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body())))
            .containsEntry("username", "Username is already taken.")
            .containsEntry("emailAddress", "Email Address is already taken.");
    }

    @Test
    void disabledUsersStillReserveTheirUsernameAndEmailAddress() throws Exception {
        UserEntity disabledUser = new UserEntity();
        disabledUser.setUsername("Dormant");
        disabledUser.setUsernameNormalized("dormant");
        disabledUser.setEmailAddress("dormant@example.com");
        disabledUser.setEmailAddressNormalized("dormant@example.com");
        disabledUser.setPasswordHash(passwordEncoder.encode("password123"));
        disabledUser.setRole(Role.USER);
        disabledUser.setAccountStatus(AccountStatus.DISABLED);
        userRepository.save(disabledUser);

        HttpResponse<String> response = signup(Map.of(
            "username", "DORMANT",
            "emailAddress", "Dormant@example.com",
            "password", "password456"
        ));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body())))
            .containsEntry("username", "Username is already taken.")
            .containsEntry("emailAddress", "Email Address is already taken.");
    }

    @Test
    void signupReturnsAllBasicFieldValidationErrorsTogether() throws Exception {
        HttpResponse<String> response = signup(Map.of(
            "username", " a ",
            "emailAddress", " not-an-email ",
            "password", "short"
        ));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body())))
            .containsEntry("username", "Username must be 3 to 30 characters long.")
            .containsEntry("emailAddress", "Email Address must be valid.")
            .containsEntry("password", "Password must be at least 8 characters long.");
    }

    @Test
    void seededAdminExistsWithoutUsingPublicSignup() {
        UserEntity adminUser = userRepository.findByUsernameNormalized("admin").orElseThrow();

        assertThat(adminUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(adminUser.getAccountStatus()).isEqualTo(AccountStatus.ENABLED);
        assertThat(adminUser.getEmailAddress()).isEqualTo("admin@promptvault.local");
    }

    private HttpResponse<String> signup(Map<String, String> payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/signup"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Object> readJson(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractFieldMessages(Map<String, Object> body) {
        return ((java.util.List<Map<String, String>>) body.get("fieldErrors"))
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                fieldError -> fieldError.get("field"),
                fieldError -> fieldError.get("message")
            ));
    }
}
