package com.promptvault.api.policykeyword;

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
class PolicyKeywordsApiTest extends AbstractMySqlIntegrationTest {

    private static final String SEEDED_ADMIN_USERNAME = "admin";
    private static final String SEEDED_ADMIN_PASSWORD = "admin-password123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PolicyKeywordRepository policyKeywordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI baseUri;

    PolicyKeywordsApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void adminsCanCreateAndListPolicyKeywords() throws Exception {
        UserEntity seededAdmin = userRepository.findByUsernameNormalized(SEEDED_ADMIN_USERNAME).orElseThrow();
        HttpClient adminClient = authenticatedClient();
        String keywordText = uniqueKeyword("Secret Token");

        HttpResponse<String> createResponse = createPolicyKeyword(adminClient, "  " + keywordText + "  ");

        assertThat(createResponse.statusCode()).isEqualTo(201);
        Map<String, Object> keyword = readJson(createResponse.body());
        assertThat(keyword).containsEntry("keyword", keywordText)
            .containsEntry("createdByUserId", seededAdmin.getId().intValue())
            .containsEntry("createdByUsername", SEEDED_ADMIN_USERNAME);
        assertPolicyKeywordShape(keyword);

        HttpResponse<String> listResponse = listPolicyKeywords(adminClient);
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(readList(listResponse.body())).anySatisfy(listedKeyword -> {
            assertThat(listedKeyword).containsEntry("id", keyword.get("id"));
            assertThat(listedKeyword).containsEntry("keyword", keywordText);
            assertPolicyKeywordShape(listedKeyword);
        });
    }

    @Test
    void blankPolicyKeywordsReturnValidationErrorsAfterTrimming() throws Exception {
        HttpClient adminClient = authenticatedClient();

        HttpResponse<String> response = createPolicyKeyword(adminClient, "   ");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(response.body()))).containsKey("keyword");
    }

    @Test
    void policyKeywordsMustBeUniqueCaseInsensitivelyOnCreateAndUpdate() throws Exception {
        HttpClient adminClient = authenticatedClient();
        String keyword = uniqueKeyword("Secret");
        assertThat(createPolicyKeyword(adminClient, keyword).statusCode()).isEqualTo(201);

        HttpResponse<String> duplicateCreateResponse = createPolicyKeyword(adminClient, " " + keyword.toUpperCase(java.util.Locale.ROOT) + " ");

        assertThat(duplicateCreateResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(duplicateCreateResponse.body()))).containsEntry("keyword", "Policy Keyword must be unique.");

        Map<String, Object> target = readJson(createPolicyKeyword(adminClient, uniqueKeyword("Target")).body());
        HttpResponse<String> duplicateUpdateResponse = updatePolicyKeyword(adminClient, (Integer) target.get("id"), keyword.toUpperCase(java.util.Locale.ROOT));

        assertThat(duplicateUpdateResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(duplicateUpdateResponse.body()))).containsEntry("keyword", "Policy Keyword must be unique.");
    }

    @Test
    void adminsCanUpdatePolicyKeywords() throws Exception {
        HttpClient adminClient = authenticatedClient();
        Map<String, Object> original = readJson(createPolicyKeyword(adminClient, uniqueKeyword("Original")).body());
        PolicyKeywordEntity originalEntity = policyKeywordRepository.findById(((Integer) original.get("id")).longValue()).orElseThrow();
        String updatedKeyword = uniqueKeyword("Updated");
        Thread.sleep(1100);

        HttpResponse<String> response = updatePolicyKeyword(adminClient, (Integer) original.get("id"), "  " + updatedKeyword + "  ");

        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> keyword = readJson(response.body());
        assertThat(keyword).containsEntry("id", original.get("id")).containsEntry("keyword", updatedKeyword);
        PolicyKeywordEntity updatedEntity = policyKeywordRepository.findById(((Integer) original.get("id")).longValue()).orElseThrow();
        assertThat(updatedEntity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
        assertThat(updatedEntity.getUpdatedAt()).isAfter(originalEntity.getUpdatedAt());
    }

    @Test
    void adminsCanDeletePolicyKeywords() throws Exception {
        HttpClient adminClient = authenticatedClient();
        Map<String, Object> keyword = readJson(createPolicyKeyword(adminClient, uniqueKeyword("Delete")).body());
        int keywordId = (Integer) keyword.get("id");

        HttpResponse<String> response = deletePolicyKeyword(adminClient, keywordId);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(policyKeywordRepository.findById((long) keywordId)).isEmpty();
    }

    @Test
    void missingPolicyKeywordTargetsReturnNotFound() throws Exception {
        HttpClient adminClient = authenticatedClient();

        assertThat(updatePolicyKeyword(adminClient, 999_999_999, uniqueKeyword("Missing")).statusCode()).isEqualTo(404);
        assertThat(deletePolicyKeyword(adminClient, 999_999_999).statusCode()).isEqualTo(404);
    }

    @Test
    void normalUsersAndUnauthenticatedVisitorsCannotManagePolicyKeywords() throws Exception {
        String password = "password123";
        UserEntity user = saveUser(uniqueUsername("policyUser"), password, Role.USER, AccountStatus.ENABLED);
        HttpClient userClient = authenticatedClient(user.getUsername(), password);
        HttpClient unauthenticatedClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        Map<String, Object> adminKeyword = readJson(createPolicyKeyword(authenticatedClient(), uniqueKeyword("Forbidden")).body());

        assertThat(listPolicyKeywords(userClient).statusCode()).isEqualTo(403);
        assertThat(createPolicyKeyword(userClient, uniqueKeyword("Forbidden Create")).statusCode()).isEqualTo(403);
        assertThat(updatePolicyKeyword(userClient, (Integer) adminKeyword.get("id"), uniqueKeyword("Forbidden Update")).statusCode()).isEqualTo(403);
        assertThat(deletePolicyKeyword(userClient, (Integer) adminKeyword.get("id")).statusCode()).isEqualTo(403);
        assertThat(listPolicyKeywords(unauthenticatedClient).statusCode()).isEqualTo(401);
        assertThat(createPolicyKeyword(unauthenticatedClient, uniqueKeyword("Unauthenticated Create")).statusCode()).isEqualTo(401);
        assertThat(updatePolicyKeyword(unauthenticatedClient, (Integer) adminKeyword.get("id"), uniqueKeyword("Unauthenticated Update")).statusCode()).isEqualTo(401);
        assertThat(deletePolicyKeyword(unauthenticatedClient, (Integer) adminKeyword.get("id")).statusCode()).isEqualTo(401);
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

    private HttpResponse<String> listPolicyKeywords(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/policy/keywords"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> createPolicyKeyword(HttpClient client, String keyword) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/policy/keywords"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("keyword", keyword))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> updatePolicyKeyword(HttpClient client, int keywordId, String keyword) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/policy/keywords/" + keywordId))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("keyword", keyword))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deletePolicyKeyword(HttpClient client, int keywordId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/policy/keywords/" + keywordId))
            .header("Accept", "application/json")
            .DELETE()
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

    private String uniqueKeyword(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void assertPolicyKeywordShape(Map<String, Object> keyword) {
        assertThat(keyword).containsOnlyKeys("id", "keyword", "createdAt", "createdByUserId", "createdByUsername", "updatedAt");
        assertThat(keyword.get("id")).isInstanceOf(Integer.class);
        assertThat(keyword.get("keyword")).isInstanceOf(String.class);
        assertThat(keyword.get("createdByUserId")).isInstanceOf(Integer.class);
        assertThat(keyword.get("createdByUsername")).isInstanceOf(String.class);
        assertThat(OffsetDateTime.parse((String) keyword.get("createdAt"))).isNotNull();
        assertThat(OffsetDateTime.parse((String) keyword.get("updatedAt"))).isNotNull();
    }
}
