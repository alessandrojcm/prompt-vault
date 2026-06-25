package com.promptvault.api.promptcategory;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
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
class PromptCategoriesApiTest extends AbstractMySqlIntegrationTest {

    private static final String SEEDED_ADMIN_USERNAME = "admin";
    private static final String SEEDED_ADMIN_PASSWORD = "admin-password123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI baseUri;

    PromptCategoriesApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void authenticatedUsersCanFetchSeededPromptCategories() throws Exception {
        UserEntity seededAdmin = userRepository.findByUsernameNormalized(SEEDED_ADMIN_USERNAME).orElseThrow();
        HttpClient client = authenticatedClient();

        HttpResponse<String> response = listPromptCategories(client);

        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> categories = readList(response.body());
        assertThat(categories).extracting(category -> category.get("label"))
            .contains("Coding", "Cybersecurity", "HR", "Legal", "Personal Productivity", "Research");
        assertThat(categories).extracting(category -> category.get("slug"))
            .contains("coding", "cybersecurity", "hr", "legal", "personal_productivity", "research");
        assertThat(categories).allSatisfy(category -> assertCategoryShape(category, seededAdmin));
    }

    @Test
    void unauthenticatedCallersCannotFetchPromptCategories() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        HttpResponse<String> response = listPromptCategories(client);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void adminsCanCreatePromptCategories() throws Exception {
        UserEntity seededAdmin = userRepository.findByUsernameNormalized(SEEDED_ADMIN_USERNAME).orElseThrow();
        HttpClient adminClient = authenticatedClient();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        HttpResponse<String> response = createPromptCategory(adminClient, "  Ops / R&D " + suffix + "  ");

        assertThat(response.statusCode()).isEqualTo(201);
        Map<String, Object> category = readJson(response.body());
        assertThat(category).containsEntry("label", "Ops / R&D " + suffix);
        assertThat(category).containsEntry("slug", "ops_r_d_" + suffix);
        assertThat(category).containsEntry("createdByUserId", seededAdmin.getId().intValue());
        assertCategoryShape(category, seededAdmin);
    }

    @Test
    void categoryLabelsMustBeUniqueCaseInsensitively() throws Exception {
        HttpClient adminClient = authenticatedClient();
        String label = uniqueLabel("Case Unique");
        assertThat(createPromptCategory(adminClient, label).statusCode()).isEqualTo(201);

        HttpResponse<String> response = createPromptCategory(adminClient, label.toUpperCase(java.util.Locale.ROOT));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body()))).containsEntry("label", "Prompt Category label must be unique.");
    }

    @Test
    void generatedSlugConflictsReturnLabelValidationErrors() throws Exception {
        HttpClient adminClient = authenticatedClient();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        assertThat(createPromptCategory(adminClient, "AI Tools " + suffix).statusCode()).isEqualTo(201);

        HttpResponse<String> response = createPromptCategory(adminClient, "AI-Tools-" + suffix);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body()))).containsEntry("label", "Prompt Category slug generated from label must be unique.");
    }

    @Test
    void blankLabelsReturnValidationErrorsAfterTrimming() throws Exception {
        HttpClient adminClient = authenticatedClient();

        HttpResponse<String> response = createPromptCategory(adminClient, "   ");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body()))).containsKey("label");
    }

    @Test
    void labelsAreLimitedToOneHundredCharacters() throws Exception {
        HttpClient adminClient = authenticatedClient();

        HttpResponse<String> response = createPromptCategory(adminClient, "a".repeat(101));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body()))).containsKey("label");
    }

    @Test
    void normalUsersCannotCreatePromptCategories() throws Exception {
        String password = "password123";
        UserEntity user = saveUser(uniqueUsername("categoryUser"), password, Role.USER, AccountStatus.ENABLED);
        HttpClient userClient = authenticatedClient(user.getUsername(), password);

        HttpResponse<String> response = createPromptCategory(userClient, uniqueLabel("Forbidden"));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void unauthenticatedCallersCannotCreatePromptCategories() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        HttpResponse<String> response = createPromptCategory(client, uniqueLabel("Unauthenticated"));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpClient authenticatedClient() throws Exception {
        return authenticatedClient(SEEDED_ADMIN_USERNAME, SEEDED_ADMIN_PASSWORD);
    }

    private HttpClient authenticatedClient(String username, String password) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> loginResponse = login(client, username, password);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return client;
    }

    private HttpResponse<String> login(HttpClient client, String username, String password) throws Exception {
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

    private HttpResponse<String> listPromptCategories(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompt/categories"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> createPromptCategory(HttpClient client, String label) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompt/categories"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("label", label))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
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

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueLabel(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void assertCategoryShape(Map<String, Object> category, UserEntity seededAdmin) {
        assertThat(category).containsOnlyKeys("id", "label", "slug", "createdAt", "createdByUserId", "updatedAt");
        assertThat(category.get("id")).isInstanceOf(Integer.class);
        assertThat(category.get("label")).isInstanceOf(String.class);
        assertThat(category.get("slug")).isInstanceOf(String.class);
        assertThat(category).containsEntry("createdByUserId", seededAdmin.getId().intValue());
        assertThat(OffsetDateTime.parse((String) category.get("createdAt"))).isNotNull();
        assertThat(OffsetDateTime.parse((String) category.get("updatedAt"))).isNotNull();
    }
}
