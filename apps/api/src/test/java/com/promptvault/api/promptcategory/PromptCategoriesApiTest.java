package com.promptvault.api.promptcategory;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptvault.api.support.AbstractMySqlIntegrationTest;
import com.promptvault.api.user.UserEntity;
import com.promptvault.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PromptCategoriesApiTest extends AbstractMySqlIntegrationTest {

    private static final String SEEDED_ADMIN_USERNAME = "admin";
    private static final String SEEDED_ADMIN_PASSWORD = "admin-password123";

    @Autowired
    private UserRepository userRepository;

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
        assertThat(categories).hasSize(6);
        assertThat(categories).extracting(category -> category.get("label"))
            .containsExactly("Coding", "Cybersecurity", "HR", "Legal", "Personal Productivity", "Research");
        assertThat(categories).extracting(category -> category.get("slug"))
            .containsExactly("coding", "cybersecurity", "hr", "legal", "personal_productivity", "research");
        assertThat(categories).allSatisfy(category -> assertCategoryShape(category, seededAdmin));
    }

    @Test
    void unauthenticatedCallersCannotFetchPromptCategories() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        HttpResponse<String> response = listPromptCategories(client);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpClient authenticatedClient() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> loginResponse = login(client);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return client;
    }

    private HttpResponse<String> login(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/login"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of(
                "username", SEEDED_ADMIN_USERNAME,
                "password", SEEDED_ADMIN_PASSWORD
            ))))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> listPromptCategories(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/prompt-categories"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<Map<String, Object>> readList(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
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
