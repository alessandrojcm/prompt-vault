package com.promptvault.api.prompt;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class PromptsApiTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromptCategoryRepository promptCategoryRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI baseUri;

    PromptsApiTest(@Value("${local.server.port}") int port) {
        this.baseUri = URI.create("http://127.0.0.1:" + port);
    }

    @Test
    void authenticatedUsersCanCreatePrivatePromptsWithNormalizedContentAndDuplicateTitles() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser user = createUser();
        HttpClient client = authenticatedClient(user);

        String duplicateTitle = "Repeatable title";
        HttpResponse<String> response = createPrompt(client, Map.of(
            "title", "  " + duplicateTitle + "  ",
            "text", "  First line\n\n  Second line  ",
            "categoryId", category.getId()
        ));
        HttpResponse<String> duplicateResponse = createPrompt(client, Map.of(
            "title", duplicateTitle,
            "text", "Different text",
            "categoryId", category.getId()
        ));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(duplicateResponse.statusCode()).isEqualTo(201);

        Map<String, Object> body = readJson(response.body());
        assertThat(body).containsEntry("title", duplicateTitle)
            .containsEntry("text", "First line\n\n  Second line")
            .containsEntry("visibility", "PRIVATE")
            .containsEntry("categoryId", category.getId().intValue())
            .containsEntry("ownerUserId", user.entity().getId().intValue());
        assertThat(OffsetDateTime.parse((String) body.get("createdAt"))).isNotNull();
        assertThat(OffsetDateTime.parse((String) body.get("updatedAt"))).isNotNull();

        Long promptId = ((Number) body.get("id")).longValue();
        PromptEntity persistedPrompt = promptRepository.findAllByOwnerIdOrderByCreatedAtDescIdDesc(user.entity().getId())
            .stream()
            .filter(prompt -> prompt.getId().equals(promptId))
            .findFirst()
            .orElseThrow();
        assertThat(persistedPrompt.getTitle()).isEqualTo(duplicateTitle);
        assertThat(persistedPrompt.getText()).isEqualTo("First line\n\n  Second line");
        assertThat(persistedPrompt.getVisibility()).isEqualTo(PromptVisibility.PRIVATE);
        assertThat(persistedPrompt.getOwner().getId()).isEqualTo(user.entity().getId());
        assertThat(persistedPrompt.getCategory().getId()).isEqualTo(category.getId());
        assertThat(persistedPrompt.getCreatedAt()).isNotNull();
        assertThat(persistedPrompt.getUpdatedAt()).isNotNull();
    }

    @Test
    void promptCreationReturnsValidationErrorsForTrimmedBlankOversizedMissingAndUnknownCategories() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        HttpClient client = authenticatedClient(createUser());

        HttpResponse<String> blankResponse = createPrompt(client, Map.of(
            "title", "   ",
            "text", "  ",
            "categoryId", category.getId()
        ));
        HttpResponse<String> oversizedResponse = createPrompt(client, Map.of(
            "title", "t".repeat(121),
            "text", "x".repeat(10001),
            "categoryId", category.getId()
        ));
        HttpResponse<String> missingCategoryResponse = createPromptJson(client, """
            {"title":"Valid title","text":"Valid text"}
            """);
        HttpResponse<String> unknownCategoryResponse = createPrompt(client, Map.of(
            "title", "Valid title",
            "text", "Valid text",
            "categoryId", 999_999_999
        ));

        assertThat(blankResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(blankResponse.body())))
            .containsEntry("title", "Prompt Title must be 1 to 120 characters long.")
            .containsEntry("text", "Prompt Text must be 1 to 10,000 characters long.");
        assertThat(oversizedResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(oversizedResponse.body())))
            .containsEntry("title", "Prompt Title must be 1 to 120 characters long.")
            .containsEntry("text", "Prompt Text must be 1 to 10,000 characters long.");
        assertThat(missingCategoryResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(missingCategoryResponse.body())))
            .containsEntry("categoryId", "Prompt Category is required.");
        assertThat(unknownCategoryResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(unknownCategoryResponse.body())))
            .containsEntry("categoryId", "Prompt Category must exist.");
    }

    @Test
    void myPromptsReturnsOnlyPromptsOwnedByTheCurrentUserIncludingPrivatePrompts() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser owner = createUser();
        TestUser otherUser = createUser();
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient otherClient = authenticatedClient(otherUser);

        Map<String, Object> firstPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Owner private one",
            "text", "Owned text one",
            "categoryId", category.getId()
        )).body());
        Map<String, Object> secondPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Owner private two",
            "text", "Owned text two",
            "categoryId", category.getId()
        )).body());
        createPrompt(otherClient, Map.of(
            "title", "Other private prompt",
            "text", "Other text",
            "categoryId", category.getId()
        ));

        HttpResponse<String> response = listMyPrompts(ownerClient, owner.entity().getId());
        HttpResponse<String> otherUserListResponse = listMyPrompts(ownerClient, otherUser.entity().getId());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(otherUserListResponse.statusCode()).isEqualTo(403);
        List<Map<String, Object>> prompts = readList(response.body());
        assertThat(prompts).extracting(prompt -> prompt.get("id"))
            .contains(((Number) firstPrompt.get("id")).intValue(), ((Number) secondPrompt.get("id")).intValue());
        assertThat(prompts).allSatisfy(prompt -> {
            assertThat(prompt).containsEntry("ownerUserId", owner.entity().getId().intValue());
            assertThat(prompt).containsEntry("visibility", "PRIVATE");
        });
        assertThat(prompts).extracting(prompt -> prompt.get("title"))
            .doesNotContain("Other private prompt");
    }

    @Test
    void promptOwnersCanRetrieveAndUpdateOwnedPromptDetails() throws Exception {
        List<PromptCategoryEntity> categories = promptCategoryRepository.findAllByOrderByLabelAsc();
        PromptCategoryEntity originalCategory = categories.getFirst();
        PromptCategoryEntity updatedCategory = categories.getLast();
        TestUser owner = createUser();
        TestUser otherUser = createUser();
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient otherClient = authenticatedClient(otherUser);

        Map<String, Object> createdPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Original title",
            "text", "Original text",
            "categoryId", originalCategory.getId()
        )).body());
        Long promptId = ((Number) createdPrompt.get("id")).longValue();

        HttpResponse<String> detailResponse = getPrompt(ownerClient, promptId);
        HttpResponse<String> otherUserDetailResponse = getPrompt(otherClient, promptId);
        HttpResponse<String> updateResponse = updatePrompt(ownerClient, promptId, Map.of(
            "title", "  Updated title  ",
            "text", "  Updated text\n\n  still here  ",
            "categoryId", updatedCategory.getId()
        ));

        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(detailResponse.body())).containsEntry("id", promptId.intValue())
            .containsEntry("title", "Original title")
            .containsEntry("text", "Original text")
            .containsEntry("categoryId", originalCategory.getId().intValue())
            .containsEntry("ownerUserId", owner.entity().getId().intValue());
        assertThat(otherUserDetailResponse.statusCode()).isEqualTo(404);
        assertThat(updateResponse.statusCode()).isEqualTo(200);

        Map<String, Object> updatedPrompt = readJson(updateResponse.body());
        assertThat(updatedPrompt).containsEntry("id", promptId.intValue())
            .containsEntry("title", "Updated title")
            .containsEntry("text", "Updated text\n\n  still here")
            .containsEntry("categoryId", updatedCategory.getId().intValue())
            .containsEntry("ownerUserId", owner.entity().getId().intValue());
        assertThat(OffsetDateTime.parse((String) updatedPrompt.get("updatedAt")))
            .isAfter(OffsetDateTime.parse((String) createdPrompt.get("updatedAt")));

        PromptEntity persistedPrompt = promptRepository.findById(promptId).orElseThrow();
        assertThat(persistedPrompt.getTitle()).isEqualTo("Updated title");
        assertThat(persistedPrompt.getText()).isEqualTo("Updated text\n\n  still here");
        assertThat(persistedPrompt.getCategory().getId()).isEqualTo(updatedCategory.getId());
    }

    @Test
    void promptUpdatesReturnValidationErrorsForTrimmedBlankOversizedMissingAndUnknownCategories() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        HttpClient client = authenticatedClient(createUser());
        Long promptId = ((Number) readJson(createPrompt(client, Map.of(
            "title", "Valid title",
            "text", "Valid text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();

        HttpResponse<String> blankResponse = updatePrompt(client, promptId, Map.of(
            "title", "   ",
            "text", "  ",
            "categoryId", category.getId()
        ));
        HttpResponse<String> oversizedResponse = updatePrompt(client, promptId, Map.of(
            "title", "t".repeat(121),
            "text", "x".repeat(10001),
            "categoryId", category.getId()
        ));
        HttpResponse<String> missingCategoryResponse = updatePromptJson(client, promptId, """
            {"title":"Valid title","text":"Valid text"}
            """);
        HttpResponse<String> unknownCategoryResponse = updatePrompt(client, promptId, Map.of(
            "title", "Valid title",
            "text", "Valid text",
            "categoryId", 999_999_999
        ));

        assertThat(blankResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(blankResponse.body())))
            .containsEntry("title", "Prompt Title must be 1 to 120 characters long.")
            .containsEntry("text", "Prompt Text must be 1 to 10,000 characters long.");
        assertThat(oversizedResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(oversizedResponse.body())))
            .containsEntry("title", "Prompt Title must be 1 to 120 characters long.")
            .containsEntry("text", "Prompt Text must be 1 to 10,000 characters long.");
        assertThat(missingCategoryResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(missingCategoryResponse.body())))
            .containsEntry("categoryId", "Prompt Category is required.");
        assertThat(unknownCategoryResponse.statusCode()).isEqualTo(400);
        assertThat(extractFieldMessages(readJson(unknownCategoryResponse.body())))
            .containsEntry("categoryId", "Prompt Category must exist.");
    }

    @Test
    void promptOwnersCanDeleteOwnedPromptsWithoutDeletingOwnerOrCategory() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser owner = createUser();
        TestUser otherUser = createUser();
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient otherClient = authenticatedClient(otherUser);
        Long promptId = ((Number) readJson(createPrompt(ownerClient, Map.of(
            "title", "Delete me",
            "text", "Delete text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();

        HttpResponse<String> otherUserDeleteResponse = deletePrompt(otherClient, promptId);
        HttpResponse<String> deleteResponse = deletePrompt(ownerClient, promptId);
        HttpResponse<String> detailAfterDeleteResponse = getPrompt(ownerClient, promptId);
        HttpResponse<String> listAfterDeleteResponse = listMyPrompts(ownerClient, owner.entity().getId());

        assertThat(otherUserDeleteResponse.statusCode()).isEqualTo(404);
        assertThat(deleteResponse.statusCode()).isEqualTo(204);
        assertThat(detailAfterDeleteResponse.statusCode()).isEqualTo(404);
        assertThat(readList(listAfterDeleteResponse.body()))
            .extracting(prompt -> prompt.get("id"))
            .doesNotContain(promptId.intValue());
        assertThat(promptRepository.findById(promptId)).isEmpty();
        assertThat(promptCategoryRepository.findById(category.getId())).isPresent();
        assertThat(userRepository.findById(owner.entity().getId())).isPresent();
    }

    @Test
    void missingPromptIdsReturnNotFoundForDetailUpdateAndDelete() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        HttpClient client = authenticatedClient(createUser());
        long missingPromptId = 999_999_999L;

        HttpResponse<String> detailResponse = getPrompt(client, missingPromptId);
        HttpResponse<String> updateResponse = updatePrompt(client, missingPromptId, Map.of(
            "title", "Valid title",
            "text", "Valid text",
            "categoryId", category.getId()
        ));
        HttpResponse<String> deleteResponse = deletePrompt(client, missingPromptId);

        assertThat(detailResponse.statusCode()).isEqualTo(404);
        assertThat(updateResponse.statusCode()).isEqualTo(404);
        assertThat(deleteResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void unauthenticatedCallersCannotCreateOrListPrompts() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        HttpResponse<String> createResponse = createPrompt(client, Map.of(
            "title", "Blocked",
            "text", "Blocked text",
            "categoryId", category.getId()
        ));
        HttpResponse<String> listResponse = listMyPrompts(client, 1L);
        HttpResponse<String> detailResponse = getPrompt(client, 1L);
        HttpResponse<String> updateResponse = updatePrompt(client, 1L, Map.of(
            "title", "Blocked",
            "text", "Blocked text",
            "categoryId", category.getId()
        ));
        HttpResponse<String> deleteResponse = deletePrompt(client, 1L);

        assertThat(createResponse.statusCode()).isEqualTo(401);
        assertThat(listResponse.statusCode()).isEqualTo(401);
        assertThat(detailResponse.statusCode()).isEqualTo(401);
        assertThat(updateResponse.statusCode()).isEqualTo(401);
        assertThat(deleteResponse.statusCode()).isEqualTo(401);
    }

    private TestUser createUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "user" + suffix;
        String password = "password123";

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(username);
        user.setEmailAddress(username + "@example.com");
        user.setEmailAddressNormalized(username + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ENABLED);
        return new TestUser(userRepository.save(user), password);
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
        return createPromptJson(client, objectMapper.writeValueAsString(payload));
    }

    private HttpResponse<String> createPromptJson(HttpClient client, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> listMyPrompts(HttpClient client, Long userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/users/" + userId + "/prompts"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getPrompt(HttpClient client, Long promptId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> updatePrompt(HttpClient client, Long promptId, Map<String, Object> payload) throws Exception {
        return updatePromptJson(client, promptId, objectMapper.writeValueAsString(payload));
    }

    private HttpResponse<String> updatePromptJson(HttpClient client, Long promptId, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deletePrompt(HttpClient client, Long promptId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId))
            .DELETE()
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
    private Map<String, String> extractFieldMessages(Map<String, Object> body) {
        return ((List<Map<String, String>>) body.get("fieldErrors"))
            .stream()
            .collect(Collectors.toMap(
                fieldError -> fieldError.get("field"),
                fieldError -> fieldError.get("message")
            ));
    }

    private record TestUser(UserEntity entity, String password) {
    }
}
