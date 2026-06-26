package com.promptvault.api.prompt;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.api.policykeyword.PolicyKeywordEntity;
import com.promptvault.api.policykeyword.PolicyKeywordRepository;
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
    private PromptFlagRepository promptFlagRepository;

    @Autowired
    private PolicyKeywordRepository policyKeywordRepository;

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
    void promptCreationRecordsFlagForAllMatchingPolicyKeywordsFromTextOnly() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser admin = createAdmin();
        TestUser owner = createUser();
        String suffix = uniqueSuffix();
        createPolicyKeyword("secret " + suffix, admin.entity());
        createPolicyKeyword("API secret " + suffix, admin.entity());
        createPolicyKeyword("Internal   Phrase " + suffix, admin.entity());
        createPolicyKeyword("title-only " + suffix, admin.entity());
        HttpClient ownerClient = authenticatedClient(owner);

        HttpResponse<String> createResponse = createPrompt(ownerClient, Map.of(
            "title", "title-only " + suffix + " should not be scanned",
            "text", "This API SECRET " + suffix + " includes an Internal   Phrase " + suffix + ".",
            "categoryId", category.getId()
        ));

        assertThat(createResponse.statusCode()).isEqualTo(201);
        Map<String, Object> createdPrompt = readJson(createResponse.body());
        Long promptId = ((Number) createdPrompt.get("id")).longValue();
        assertThat(OffsetDateTime.parse((String) createdPrompt.get("flaggedAt"))).isNotNull();
        assertThat(createdPrompt).doesNotContainKeys("matchedKeywords", "keywordSnapshots");

        PromptFlagEntity flag = promptFlagRepository.findByPromptId(promptId).orElseThrow();
        assertThat(flag.getFlaggedAt()).isNotNull();
        assertThat(flag.getKeywordSnapshots())
            .extracting(PromptFlagKeywordSnapshotEntity::getKeywordText)
            .containsExactly("API secret " + suffix, "Internal   Phrase " + suffix, "secret " + suffix);
    }

    @Test
    void promptCreationWithoutTextMatchesRecordsNoFlagAndOwnedReadsExposeFlaggedAtOnlyWhenFlagged() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser admin = createAdmin();
        TestUser owner = createUser();
        String keyword = "secret " + uniqueSuffix();
        createPolicyKeyword(keyword, admin.entity());
        HttpClient ownerClient = authenticatedClient(owner);

        Map<String, Object> unflaggedPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", keyword + " appears only in the title",
            "text", "Public-safe body",
            "categoryId", category.getId()
        )).body());
        Map<String, Object> flaggedPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Text match",
            "text", "Body contains " + keyword.toUpperCase(Locale.ROOT) + " content",
            "categoryId", category.getId()
        )).body());
        Long unflaggedPromptId = ((Number) unflaggedPrompt.get("id")).longValue();
        Long flaggedPromptId = ((Number) flaggedPrompt.get("id")).longValue();

        HttpResponse<String> listResponse = listMyPrompts(ownerClient, owner.entity().getId());
        HttpResponse<String> flaggedDetailResponse = getPrompt(ownerClient, flaggedPromptId);
        HttpResponse<String> unflaggedDetailResponse = getPrompt(ownerClient, unflaggedPromptId);

        assertThat(promptFlagRepository.findByPromptId(unflaggedPromptId)).isEmpty();
        assertThat(promptFlagRepository.findByPromptId(flaggedPromptId)).isPresent();
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(readList(listResponse.body()))
            .filteredOn(prompt -> prompt.get("id").equals(flaggedPromptId.intValue()))
            .singleElement()
            .satisfies(prompt -> {
                assertThat(OffsetDateTime.parse((String) prompt.get("flaggedAt"))).isNotNull();
                assertThat(prompt).doesNotContainKeys("matchedKeywords", "keywordSnapshots");
            });
        assertThat(readList(listResponse.body()))
            .filteredOn(prompt -> prompt.get("id").equals(unflaggedPromptId.intValue()))
            .singleElement()
            .satisfies(prompt -> assertThat(prompt).containsEntry("flaggedAt", null)
                .doesNotContainKeys("matchedKeywords", "keywordSnapshots"));

        assertThat(flaggedDetailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(flaggedDetailResponse.body())).containsKey("flaggedAt")
            .doesNotContainKeys("matchedKeywords", "keywordSnapshots");
        assertThat(unflaggedDetailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(unflaggedDetailResponse.body()))
            .containsEntry("flaggedAt", null)
            .doesNotContainKeys("matchedKeywords", "keywordSnapshots");
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
    void promptOwnersCanShareAndUnshareOwnedPrompts() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser owner = createUser();
        HttpClient ownerClient = authenticatedClient(owner);

        Map<String, Object> createdPrompt = readJson(createPrompt(ownerClient, Map.of(
            "title", "Shareable prompt",
            "text", "Shareable text",
            "categoryId", category.getId()
        )).body());
        Long promptId = ((Number) createdPrompt.get("id")).longValue();

        HttpResponse<String> shareResponse = updatePromptVisibility(ownerClient, promptId, "PUBLIC");
        HttpResponse<String> listSharedResponse = listMyPrompts(ownerClient, owner.entity().getId());
        HttpResponse<String> unshareResponse = updatePromptVisibility(ownerClient, promptId, "PRIVATE");
        HttpResponse<String> listUnsharedResponse = listMyPrompts(ownerClient, owner.entity().getId());

        assertThat(shareResponse.statusCode()).isEqualTo(200);
        Map<String, Object> sharedPrompt = readJson(shareResponse.body());
        assertThat(sharedPrompt).containsEntry("id", promptId.intValue())
            .containsEntry("visibility", "PUBLIC")
            .containsEntry("ownerUserId", owner.entity().getId().intValue());
        assertThat(OffsetDateTime.parse((String) sharedPrompt.get("updatedAt")))
            .isAfter(OffsetDateTime.parse((String) createdPrompt.get("updatedAt")));
        assertThat(readList(listSharedResponse.body()))
            .filteredOn(prompt -> prompt.get("id").equals(promptId.intValue()))
            .extracting(prompt -> prompt.get("visibility"))
            .containsExactly("PUBLIC");

        assertThat(unshareResponse.statusCode()).isEqualTo(200);
        Map<String, Object> unsharedPrompt = readJson(unshareResponse.body());
        assertThat(unsharedPrompt).containsEntry("id", promptId.intValue())
            .containsEntry("visibility", "PRIVATE")
            .containsEntry("ownerUserId", owner.entity().getId().intValue());
        assertThat(OffsetDateTime.parse((String) unsharedPrompt.get("updatedAt")))
            .isAfter(OffsetDateTime.parse((String) sharedPrompt.get("updatedAt")));
        assertThat(readList(listUnsharedResponse.body()))
            .filteredOn(prompt -> prompt.get("id").equals(promptId.intValue()))
            .extracting(prompt -> prompt.get("visibility"))
            .containsExactly("PRIVATE");

        PromptEntity persistedPrompt = promptRepository.findById(promptId).orElseThrow();
        assertThat(persistedPrompt.getVisibility()).isEqualTo(PromptVisibility.PRIVATE);
    }

    @Test
    void authenticatedUsersCanListAndRetrievePublicPromptsFromOtherEnabledUsersOnly() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser viewer = createUser();
        TestUser owner = createUser();
        TestUser privateOwner = createUser();
        HttpClient viewerClient = authenticatedClient(viewer);
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient privateOwnerClient = authenticatedClient(privateOwner);

        Long publicPromptId = ((Number) readJson(createPrompt(ownerClient, Map.of(
            "title", "Visible public prompt",
            "text", "Visible public text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();
        updatePromptVisibility(ownerClient, publicPromptId, "PUBLIC");
        Long ownPublicPromptId = ((Number) readJson(createPrompt(viewerClient, Map.of(
            "title", "Viewer own public prompt",
            "text", "Viewer own public text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();
        updatePromptVisibility(viewerClient, ownPublicPromptId, "PUBLIC");
        Long privatePromptId = ((Number) readJson(createPrompt(privateOwnerClient, Map.of(
            "title", "Private other prompt",
            "text", "Private other text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();

        HttpResponse<String> listResponse = listPublicPrompts(viewerClient);
        HttpResponse<String> detailResponse = getPublicPrompt(viewerClient, publicPromptId);
        HttpResponse<String> ownPublicDetailResponse = getPublicPrompt(viewerClient, ownPublicPromptId);
        HttpResponse<String> privateDetailResponse = getPublicPrompt(viewerClient, privatePromptId);

        assertThat(listResponse.statusCode()).isEqualTo(200);
        List<Map<String, Object>> prompts = readList(listResponse.body());
        assertThat(prompts).filteredOn(prompt -> prompt.get("id").equals(publicPromptId.intValue()))
            .singleElement()
            .satisfies(prompt -> {
                assertThat(prompt).containsEntry("title", "Visible public prompt")
                    .containsEntry("text", "Visible public text")
                    .containsEntry("visibility", "PUBLIC")
                    .containsEntry("categoryId", category.getId().intValue())
                    .containsEntry("ownerUsername", owner.entity().getUsername());
                assertThat(prompt).doesNotContainKeys("ownerUserId", "emailAddress");
                assertThat(prompt).doesNotContainValue(owner.entity().getEmailAddress());
            });
        assertThat(prompts).extracting(prompt -> prompt.get("id"))
            .doesNotContain(ownPublicPromptId.intValue(), privatePromptId.intValue());
        assertThat(listResponse.body()).doesNotContain(owner.entity().getEmailAddress(), "emailAddress");

        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(detailResponse.body()))
            .containsEntry("id", publicPromptId.intValue())
            .containsEntry("ownerUsername", owner.entity().getUsername())
            .doesNotContainKeys("ownerUserId", "emailAddress");
        assertThat(detailResponse.body()).doesNotContain(owner.entity().getEmailAddress(), "emailAddress");
        assertThat(ownPublicDetailResponse.statusCode()).isEqualTo(404);
        assertThat(privateDetailResponse.statusCode()).isEqualTo(404);
        assertThat(privateDetailResponse.body()).doesNotContain("Private other prompt", "Private other text");
    }

    @Test
    void publicReadsHideDisabledOwnersPromptsWithoutDeletingThemAndShowOwnerEditsImmediately() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser viewer = createUser();
        TestUser owner = createUser();
        TestUser admin = createAdmin();
        HttpClient viewerClient = authenticatedClient(viewer);
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient adminClient = authenticatedClient(admin);

        Long promptId = ((Number) readJson(createPrompt(ownerClient, Map.of(
            "title", "Original public title",
            "text", "Original public text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();
        updatePromptVisibility(ownerClient, promptId, "PUBLIC");
        HttpResponse<String> initialDetailResponse = getPublicPrompt(viewerClient, promptId);

        updatePrompt(ownerClient, promptId, Map.of(
            "title", "Updated public title",
            "text", "Updated public text",
            "categoryId", category.getId()
        ));
        HttpResponse<String> updatedDetailResponse = getPublicPrompt(viewerClient, promptId);

        HttpResponse<String> disableOwnerResponse = updateUserStatus(adminClient, owner.entity().getId(), "DISABLED");
        HttpResponse<String> listAfterDisableResponse = listPublicPrompts(viewerClient);
        HttpResponse<String> detailAfterDisableResponse = getPublicPrompt(viewerClient, promptId);
        HttpResponse<String> disabledOwnerPromptApiResponse = listPublicPrompts(ownerClient);

        assertThat(initialDetailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(initialDetailResponse.body())).containsEntry("title", "Original public title");
        assertThat(updatedDetailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(updatedDetailResponse.body()))
            .containsEntry("title", "Updated public title")
            .containsEntry("text", "Updated public text");

        assertThat(disableOwnerResponse.statusCode()).isEqualTo(200);
        assertThat(listAfterDisableResponse.statusCode()).isEqualTo(200);
        assertThat(readList(listAfterDisableResponse.body())).extracting(prompt -> prompt.get("id"))
            .doesNotContain(promptId.intValue());
        assertThat(detailAfterDisableResponse.statusCode()).isEqualTo(404);
        assertThat(detailAfterDisableResponse.body()).doesNotContain("Updated public title", "Updated public text");
        assertThat(disabledOwnerPromptApiResponse.statusCode()).isEqualTo(401);

        PromptEntity persistedPrompt = promptRepository.findById(promptId).orElseThrow();
        assertThat(persistedPrompt.getTitle()).isEqualTo("Updated public title");
        assertThat(persistedPrompt.getText()).isEqualTo("Updated public text");
        assertThat(persistedPrompt.getVisibility()).isEqualTo(PromptVisibility.PUBLIC);
        assertThat(persistedPrompt.getOwner().getId()).isEqualTo(owner.entity().getId());
    }

    @Test
    void nonOwnersAndAdminsCannotShareOrUnsharePromptsOwnedByOtherUsers() throws Exception {
        PromptCategoryEntity category = promptCategoryRepository.findAllByOrderByLabelAsc().getFirst();
        TestUser owner = createUser();
        TestUser otherUser = createUser();
        TestUser admin = createAdmin();
        HttpClient ownerClient = authenticatedClient(owner);
        HttpClient otherClient = authenticatedClient(otherUser);
        HttpClient adminClient = authenticatedClient(admin);

        Long promptId = ((Number) readJson(createPrompt(ownerClient, Map.of(
            "title", "Owner controlled prompt",
            "text", "Private text",
            "categoryId", category.getId()
        )).body()).get("id")).longValue();

        HttpResponse<String> otherUserShareResponse = updatePromptVisibility(otherClient, promptId, "PUBLIC");
        HttpResponse<String> adminShareResponse = updatePromptVisibility(adminClient, promptId, "PUBLIC");
        HttpResponse<String> ownerShareResponse = updatePromptVisibility(ownerClient, promptId, "PUBLIC");
        HttpResponse<String> otherUserUnshareResponse = updatePromptVisibility(otherClient, promptId, "PRIVATE");
        HttpResponse<String> adminUnshareResponse = updatePromptVisibility(adminClient, promptId, "PRIVATE");

        assertThat(otherUserShareResponse.statusCode()).isEqualTo(404);
        assertThat(adminShareResponse.statusCode()).isEqualTo(404);
        assertThat(ownerShareResponse.statusCode()).isEqualTo(200);
        assertThat(otherUserUnshareResponse.statusCode()).isEqualTo(404);
        assertThat(adminUnshareResponse.statusCode()).isEqualTo(404);
        assertThat(otherUserShareResponse.body()).doesNotContain("Owner controlled prompt", "Private text");
        assertThat(adminShareResponse.body()).doesNotContain("Owner controlled prompt", "Private text");
        assertThat(otherUserUnshareResponse.body()).doesNotContain("Owner controlled prompt", "Private text");
        assertThat(adminUnshareResponse.body()).doesNotContain("Owner controlled prompt", "Private text");

        PromptEntity persistedPrompt = promptRepository.findById(promptId).orElseThrow();
        assertThat(persistedPrompt.getVisibility()).isEqualTo(PromptVisibility.PUBLIC);
        assertThat(persistedPrompt.getOwner().getId()).isEqualTo(owner.entity().getId());
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
        HttpResponse<String> otherUserUpdateResponse = updatePrompt(otherClient, promptId, Map.of(
            "title", "Other user title",
            "text", "Other user text",
            "categoryId", updatedCategory.getId()
        ));
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
        assertThat(otherUserDetailResponse.body()).doesNotContain("Original title", "Original text");
        assertThat(otherUserUpdateResponse.statusCode()).isEqualTo(404);
        assertThat(otherUserUpdateResponse.body()).doesNotContain("Original title", "Original text");
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
    void adminsCanManageTheirOwnPromptsButCannotManagePromptsOwnedByOtherUsers() throws Exception {
        List<PromptCategoryEntity> categories = promptCategoryRepository.findAllByOrderByLabelAsc();
        PromptCategoryEntity originalCategory = categories.getFirst();
        PromptCategoryEntity updatedCategory = categories.getLast();
        TestUser admin = createAdmin();
        TestUser owner = createUser();
        HttpClient adminClient = authenticatedClient(admin);
        HttpClient ownerClient = authenticatedClient(owner);

        Long adminPromptId = ((Number) readJson(createPrompt(adminClient, Map.of(
            "title", "Admin private title",
            "text", "Admin private text",
            "categoryId", originalCategory.getId()
        )).body()).get("id")).longValue();
        Long ownerPromptId = ((Number) readJson(createPrompt(ownerClient, Map.of(
            "title", "Owner secret title",
            "text", "Owner secret text",
            "categoryId", originalCategory.getId()
        )).body()).get("id")).longValue();

        HttpResponse<String> adminOwnDetailResponse = getPrompt(adminClient, adminPromptId);
        HttpResponse<String> adminOwnUpdateResponse = updatePrompt(adminClient, adminPromptId, Map.of(
            "title", "Admin updated title",
            "text", "Admin updated text",
            "categoryId", updatedCategory.getId()
        ));
        HttpResponse<String> adminOtherDetailResponse = getPrompt(adminClient, ownerPromptId);
        HttpResponse<String> adminOtherUpdateResponse = updatePrompt(adminClient, ownerPromptId, Map.of(
            "title", "Admin takeover title",
            "text", "Admin takeover text",
            "categoryId", updatedCategory.getId()
        ));
        HttpResponse<String> adminOtherDeleteResponse = deletePrompt(adminClient, ownerPromptId);
        HttpResponse<String> adminOwnDeleteResponse = deletePrompt(adminClient, adminPromptId);

        assertThat(adminOwnDetailResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(adminOwnDetailResponse.body()))
            .containsEntry("id", adminPromptId.intValue())
            .containsEntry("ownerUserId", admin.entity().getId().intValue())
            .containsEntry("text", "Admin private text");
        assertThat(adminOwnUpdateResponse.statusCode()).isEqualTo(200);
        assertThat(readJson(adminOwnUpdateResponse.body()))
            .containsEntry("id", adminPromptId.intValue())
            .containsEntry("title", "Admin updated title")
            .containsEntry("text", "Admin updated text")
            .containsEntry("ownerUserId", admin.entity().getId().intValue());
        assertThat(adminOwnDeleteResponse.statusCode()).isEqualTo(204);
        assertThat(promptRepository.findById(adminPromptId)).isEmpty();

        assertThat(adminOtherDetailResponse.statusCode()).isEqualTo(404);
        assertThat(adminOtherUpdateResponse.statusCode()).isEqualTo(404);
        assertThat(adminOtherDeleteResponse.statusCode()).isEqualTo(404);
        assertThat(adminOtherDetailResponse.body()).doesNotContain("Owner secret title", "Owner secret text");
        assertThat(adminOtherUpdateResponse.body()).doesNotContain("Owner secret title", "Owner secret text");
        assertThat(adminOtherDeleteResponse.body()).doesNotContain("Owner secret title", "Owner secret text");

        PromptEntity ownerPrompt = promptRepository.findById(ownerPromptId).orElseThrow();
        assertThat(ownerPrompt.getTitle()).isEqualTo("Owner secret title");
        assertThat(ownerPrompt.getText()).isEqualTo("Owner secret text");
        assertThat(ownerPrompt.getCategory().getId()).isEqualTo(originalCategory.getId());
        assertThat(ownerPrompt.getOwner().getId()).isEqualTo(owner.entity().getId());
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
        HttpResponse<String> visibilityResponse = updatePromptVisibility(client, missingPromptId, "PUBLIC");
        HttpResponse<String> deleteResponse = deletePrompt(client, missingPromptId);

        assertThat(detailResponse.statusCode()).isEqualTo(404);
        assertThat(updateResponse.statusCode()).isEqualTo(404);
        assertThat(visibilityResponse.statusCode()).isEqualTo(404);
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
        HttpResponse<String> visibilityResponse = updatePromptVisibility(client, 1L, "PUBLIC");
        HttpResponse<String> deleteResponse = deletePrompt(client, 1L);
        HttpResponse<String> publicListResponse = listPublicPrompts(client);
        HttpResponse<String> publicDetailResponse = getPublicPrompt(client, 1L);

        assertThat(createResponse.statusCode()).isEqualTo(401);
        assertThat(listResponse.statusCode()).isEqualTo(401);
        assertThat(detailResponse.statusCode()).isEqualTo(401);
        assertThat(updateResponse.statusCode()).isEqualTo(401);
        assertThat(visibilityResponse.statusCode()).isEqualTo(401);
        assertThat(deleteResponse.statusCode()).isEqualTo(401);
        assertThat(publicListResponse.statusCode()).isEqualTo(401);
        assertThat(publicDetailResponse.statusCode()).isEqualTo(401);
    }

    private TestUser createUser() {
        return createUser(Role.USER);
    }

    private TestUser createAdmin() {
        return createUser(Role.ADMIN);
    }

    private TestUser createUser(Role role) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "user" + suffix;
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

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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

    private HttpResponse<String> listPublicPrompts(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/public-prompts"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getPublicPrompt(HttpClient client, Long promptId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/public-prompts/" + promptId))
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

    private HttpResponse<String> updatePromptVisibility(HttpClient client, Long promptId, String visibility) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId + "/visibility"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                "visibility", visibility
            ))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deletePrompt(HttpClient client, Long promptId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompts/" + promptId))
            .DELETE()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> updateUserStatus(HttpClient client, Long userId, String accountStatus) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/admin/users/" + userId + "/status"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                "accountStatus", accountStatus
            ))))
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
