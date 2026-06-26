package com.promptvault.api.admin;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.api.policykeyword.PolicyKeywordEntity;
import com.promptvault.api.policykeyword.PolicyKeywordRepository;
import com.promptvault.api.prompt.PromptRepository;
import com.promptvault.api.promptcategory.PromptCategoryEntity;
import com.promptvault.api.promptcategory.PromptCategoryRepository;
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
class AdminFlaggedPromptsApiTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromptCategoryRepository promptCategoryRepository;

    @Autowired
    private PolicyKeywordRepository policyKeywordRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI baseUri;

    AdminFlaggedPromptsApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void adminsCanListCurrentFlaggedPromptsWithStableSnapshotMetadata() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser admin = createUser(Role.ADMIN);
        TestUser owner = createUser(Role.USER);
        String suffix = uniqueSuffix();
        PolicyKeywordEntity updatedKeyword = createPolicyKeyword("alpha leak " + suffix, admin.entity());
        PolicyKeywordEntity deletedKeyword = createPolicyKeyword("beta leak " + suffix, admin.entity());
        createPolicyKeyword("clearable leak " + suffix, admin.entity());
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient adminClient = authenticatedClient(admin);

        Map<String, Object> flaggedPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Needs review " + suffix,
            "text", "This includes BETA LEAK " + suffix + " and alpha leak " + suffix + ".",
            "categoryId", category.getId()
        )).body());
        readJson(createPrompt(ownerClient, Map.of(
            "title", "Unflagged title " + suffix,
            "text", "Safe prompt body",
            "categoryId", category.getId()
        )).body());
        Map<String, Object> clearedPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Cleared flag " + suffix,
            "text", "This initially contains clearable leak " + suffix + ".",
            "categoryId", category.getId()
        )).body());
        Long flaggedPromptId = ((Number) flaggedPrompt.get("id")).longValue();
        Long clearedPromptId = ((Number) clearedPrompt.get("id")).longValue();
        OffsetDateTime submittedAt = promptRepository.findById(flaggedPromptId).orElseThrow().getCreatedAt().atOffset(ZoneOffset.UTC);
        updatePrompt(ownerClient, clearedPromptId, Map.of(
            "title", "Cleared flag " + suffix,
            "text", "This body is safe now.",
            "categoryId", category.getId()
        ));
        updatedKeyword.setKeyword("changed leak " + suffix);
        updatedKeyword.setKeywordNormalized(("changed leak " + suffix).toLowerCase(Locale.ROOT));
        policyKeywordRepository.save(updatedKeyword);
        policyKeywordRepository.delete(deletedKeyword);

        HttpResponse<String> response = listFlaggedPrompts(adminClient);

        assertThat(response.statusCode()).isEqualTo(200);
        List<Map<String, Object>> prompts = readList(response.body());
        assertThat(prompts)
            .filteredOn(prompt -> prompt.get("id").equals(flaggedPromptId.intValue()))
            .singleElement()
            .satisfies(prompt -> {
                assertThat(prompt).containsEntry("title", "Needs review " + suffix)
                    .containsEntry("ownerUsername", owner.entity().getUsername())
                    .containsEntry("categoryLabel", category.getLabel());
                assertThat(OffsetDateTime.parse((String) prompt.get("submittedAt"))).isEqualTo(submittedAt);
                assertThat(matchedKeywordSnapshots(prompt))
                    .containsExactly("alpha leak " + suffix, "beta leak " + suffix);
            });
        assertThat(prompts).noneSatisfy(prompt -> assertThat(prompt.get("id")).isEqualTo(clearedPromptId.intValue()));
    }

    @Test
    void normalUsersAndUnauthenticatedVisitorsCannotListFlaggedPrompts() throws Exception {
        TestUser user = createUser(Role.USER);
        HttpClient userClient = authenticatedClient(user);
        HttpClient unauthenticatedClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        assertThat(listFlaggedPrompts(unauthenticatedClient).statusCode()).isEqualTo(401);
        assertThat(listFlaggedPrompts(userClient).statusCode()).isEqualTo(403);
    }

    private TestUser createUser(Role role) {
        String username = "user" + uniqueSuffix();
        String password = "password123";

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(username);
        user.setEmailAddress(username + "@example.com");
        user.setEmailAddressNormalized(username + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ENABLED);
        return new TestUser(userRepository.save(user), password);
    }

    private PolicyKeywordEntity createPolicyKeyword(String keyword, UserEntity createdBy) {
        PolicyKeywordEntity policyKeyword = new PolicyKeywordEntity();
        policyKeyword.setKeyword(keyword);
        policyKeyword.setKeywordNormalized(keyword.toLowerCase(Locale.ROOT));
        policyKeyword.setCreatedBy(createdBy);
        return policyKeywordRepository.save(policyKeyword);
    }

    private HttpClient authenticatedClient(TestUser user) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> loginResponse = login(client, user);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return client;
    }

    private HttpResponse<String> login(HttpClient client, TestUser user) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/login"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                "username", user.entity().getUsername(),
                "password", user.password()
            ))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> createPrompt(HttpClient client, Map<String, Object> payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> updatePrompt(HttpClient client, Long promptId, Map<String, Object> payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> listFlaggedPrompts(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/prompts/flagged"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Object> readJson(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    private List<Map<String, Object>> readList(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    @SuppressWarnings("unchecked")
    private List<String> matchedKeywordSnapshots(Map<String, Object> prompt) {
        return (List<String>) prompt.get("matchedKeywordSnapshots");
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TestUser(UserEntity entity, String password) {
    }
}
