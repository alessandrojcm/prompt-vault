# Spring Data query for submitted prompts

**Session ID:** ses_0f1bf3f7cffedHIdR7fKIXK5xv
**Created:** 6/28/2026, 1:42:32 PM
**Updated:** 6/28/2026, 1:43:45 PM

---

## User

@apps/api/src/main/java/com/promptvault/api/prompt/PromptRepository.java has a one to many to @apps/api/src/main/java/com/promptvault/api/prompt/PromptSubmissionHistoryRepository.java how would i write "Give me all the prompts that have at least one entry in the submission history that belong to the current user" to be called from @apps/api/src/main/java/com/promptvault/api/prompt/PromptsService.java 

---

## Assistant (Build · GPT-5.5 · 9.3s)

_Thinking:_

**Considering writing guidance**

I need to focus on how to respond effectively to the user's request about writing. They’ve pasted some details for context, so I'll need to inspect the entities involved. It's important to avoid direct grepping and instead use known files. I should check the documentation for Spring Data, as library usage is a key factor. Using @docs seems essential here, especially for understanding Spring Data JPA query derivation or JPQL. Reading entities may also be necessary, so I’ll use glob or read commands.

**Tool: context7_resolve-library-id**

**Input:**
```json
{
  "query": "Spring Data JPA repository query methods derived queries @Query EntityGraph exists collection relationship",
  "libraryName": "Spring Data JPA"
}
```

**Output:**
```
Available Libraries:

- Title: Spring Data JPA
- Context7-compatible library ID: /spring-projects/spring-data-jpa
- Description: Simplifies the development of creating a JPA-based data access layer. 
- Code Snippets: 285
- Source Reputation: High
- Benchmark Score: 77.49
----------
- Title: Spring Data JPA
- Context7-compatible library ID: /websites/spring_io_projects_spring-data-jpa
- Description: Spring Data JPA simplifies the implementation of JPA-based repositories, making it easier to build Spring-powered applications with data access technologies by reducing boilerplate code.
- Code Snippets: 13
- Source Reputation: High
- Benchmark Score: 2
----------
- Title: Spring Data JPA Specification DSL for Kotlin
- Context7-compatible library ID: /consoleau/kotlin-jpa-specification-dsl
- Description: This library provides a fluent DSL for querying Spring Data JPA repositories using Spring Data Specifications (i.e., the JPA Criteria API), without boilerplate code or a generated metamodel.
- Code Snippets: 28
- Source Reputation: Medium
- Benchmark Score: 55
----------
- Title: Searchable JPA
- Context7-compatible library ID: /simplecore-inc/searchable-jpa
- Description: Searchable JPA is a Spring Data JPA extension library that provides dynamic search, sorting, and high-performance cursor-based pagination with support for 20+ search operators and type-safe builder patterns.
- Code Snippets: 48
- Source Reputation: Low
- Benchmark Score: 93
----------
- Title: RSQL JPA Specification
- Context7-compatible library ID: /perplexhub/rsql-jpa-specification
- Description: A Java library that translates RSQL queries into Spring Data JPA Specifications and QueryDSL Predicates with support for entity associations.
- Code Snippets: 58
- Source Reputation: Medium
- Benchmark Score: 76
```

---

## Assistant (Build · GPT-5.5 · 5.1s)

**Tool: context7_query-docs**

**Input:**
```json
{
  "libraryId": "/spring-projects/spring-data-jpa",
  "query": "How to define repository methods using @Query JPQL with join on one-to-many collection and distinct results, plus @EntityGraph for eager associations"
}
```

**Output:**
```
### Declare Query at Query Method using @Query

Source: https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc

Defines a custom JPA query directly on the repository method using the @Query annotation. This approach takes precedence over named queries and co-locates the query with the repository interface.

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u from User u where u.emailAddress = ?1")
  User findByEmailAddress(String emailAddress);
}
```

--------------------------------

### JPQL Query with LIKE Pattern

Source: https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc

Defines a JPQL query using the @Query annotation. Spring Data JPA automatically handles LIKE pattern augmentation for method parameters.

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u from User u where u.firstname like %?1")
  List<User> findByFirstnameEndsWith(String firstname);
}
```

--------------------------------

### Using Named Parameters in Repository Query Methods

Source: https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc

Demonstrates how to use the @Param annotation to bind method parameter names to query parameters, improving refactoring safety. Note that method parameters can be switched according to their order in the query.

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u from User u where u.firstname = :firstname or u.lastname = :lastname")
  User findByLastnameOrFirstname(@Param("lastname") String lastname,
                                 @Param("firstname") String firstname);
}
```

--------------------------------

### Use Ad-hoc Entity Graph on Repository Query Method

Source: https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc

Defines an entity graph directly on a repository query method using attribute paths. This avoids the need for a separate @NamedEntityGraph annotation on the entity.

```java
public interface GroupRepository extends CrudRepository<GroupInfo, String> {

  @EntityGraph(attributePaths = { "members" })
  GroupInfo getByGroupName(String name);

}
```

--------------------------------

### Reference Named Entity Graph on Repository Query Method

Source: https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/query-methods.adoc

References a pre-defined named entity graph on a Spring Data JPA repository query method. Configures the fetch type to LOAD.

```java
public interface GroupRepository extends CrudRepository<GroupInfo, String> {

  @EntityGraph(value = "GroupInfo.detail", type = EntityGraphType.LOAD)
  GroupInfo getByGroupName(String name);

}
```
```

---

## Assistant (Build · GPT-5.5 · 6.9s)

_Thinking:_



**Tool: read**

**Input:**
```json
{
  "filePath": "/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptEntity.java",
  "offset": 1,
  "limit": 260
}
```

**Output:**
```
<path>/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptEntity.java</path>
<type>file</type>
<content>
1: package com.promptvault.api.prompt;
2: 
3: import com.promptvault.api.promptcategory.PromptCategoryEntity;
4: import com.promptvault.api.user.UserEntity;
5: import jakarta.persistence.*;
6: 
7: import java.time.Instant;
8: import java.util.List;
9: 
10: @Entity
11: @Table(name = "prompts")
12: public class PromptEntity {
13: 
14:     @Id
15:     @GeneratedValue(strategy = GenerationType.IDENTITY)
16:     private Long id;
17: 
18:     @Column(nullable = false, length = 120)
19:     private String title;
20: 
21:     @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
22:     private String text;
23: 
24:     @Enumerated(EnumType.STRING)
25:     @Column(nullable = false)
26:     private PromptVisibility visibility;
27: 
28:     @ManyToOne(fetch = FetchType.LAZY, optional = false)
29:     @JoinColumn(name = "owner_user_id", nullable = false, updatable = false)
30:     private UserEntity owner;
31: 
32:     @ManyToOne(fetch = FetchType.LAZY, optional = false)
33:     @JoinColumn(name = "category_id", nullable = false)
34:     private PromptCategoryEntity category;
35: 
36:     @Column(name = "created_at", nullable = false, updatable = false)
37:     private Instant createdAt;
38: 
39:     @Column(name = "updated_at", nullable = false)
40:     private Instant updatedAt;
41: 
42:     @OneToOne(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
43:     private PromptFlagEntity flag;
44: 
45: 
46:     @OneToMany(fetch = FetchType.LAZY, mappedBy = "prompt", cascade = CascadeType.ALL)
47:     private List<PromptSubmissionHistoryEntity> submissions;
48: 
49:     public List<PromptSubmissionHistoryEntity> getSubmissions() {
50:         return submissions;
51:     }
52: 
53:     public void setSubmissions(PromptSubmissionHistoryEntity submissions) {
54:         this.submissions.add(submissions);
55:     }
56: 
57:     public Long getId() {
58:         return id;
59:     }
60: 
61:     public String getTitle() {
62:         return title;
63:     }
64: 
65:     public void setTitle(String title) {
66:         this.title = title;
67:     }
68: 
69:     public String getText() {
70:         return text;
71:     }
72: 
73:     public void setText(String text) {
74:         this.text = text;
75:     }
76: 
77:     public PromptVisibility getVisibility() {
78:         return visibility;
79:     }
80: 
81:     public void setVisibility(PromptVisibility visibility) {
82:         this.visibility = visibility;
83:     }
84: 
85:     public UserEntity getOwner() {
86:         return owner;
87:     }
88: 
89:     public void setOwner(UserEntity owner) {
90:         this.owner = owner;
91:     }
92: 
93:     public PromptCategoryEntity getCategory() {
94:         return category;
95:     }
96: 
97:     public void setCategory(PromptCategoryEntity category) {
98:         this.category = category;
99:     }
100: 
101:     public Instant getCreatedAt() {
102:         return createdAt;
103:     }
104: 
105:     public Instant getUpdatedAt() {
106:         return updatedAt;
107:     }
108: 
109:     public PromptFlagEntity getFlag() {
110:         return flag;
111:     }
112: 
113:     public void setFlag(PromptFlagEntity flag) {
114:         if (this.flag != null && this.flag != flag) {
115:             this.flag.setPrompt(null);
116:         }
117:         this.flag = flag;
118:         if (flag != null) {
119:             flag.setPrompt(this);
120:         }
121:     }
122: 
123:     @PrePersist
124:     void onCreate() {
125:         Instant now = Instant.now();
126:         if (visibility == null) {
127:             visibility = PromptVisibility.PRIVATE;
128:         }
129:         if (createdAt == null) {
130:             createdAt = now;
131:         }
132:         if (updatedAt == null) {
133:             updatedAt = now;
134:         }
135:     }
136: 
137:     @PreUpdate
138:     void onUpdate() {
139:         updatedAt = Instant.now();
140:     }
141: }

(End of file - total 141 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptSubmissionHistoryEntity.java",
  "offset": 1,
  "limit": 220
}
```

**Output:**
```
<path>/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptSubmissionHistoryEntity.java</path>
<type>file</type>
<content>
1: package com.promptvault.api.prompt;
2: 
3: import jakarta.persistence.*;
4: import jakarta.validation.constraints.NotNull;
5: import org.hibernate.annotations.OnDelete;
6: import org.hibernate.annotations.OnDeleteAction;
7: 
8: import java.time.Instant;
9: 
10: @Entity
11: @Table(name = "prompt_submission_history", schema = "prompt_vault")
12: public class PromptSubmissionHistoryEntity {
13:     @Id
14:     @GeneratedValue(strategy = GenerationType.IDENTITY)
15:     private Long id;
16: 
17:     @NotNull
18:     @ManyToOne(fetch = FetchType.LAZY, optional = false)
19:     @OnDelete(action = OnDeleteAction.CASCADE)
20:     @JoinColumn(name = "prompt_id", nullable = false)
21:     private PromptEntity prompt;
22: 
23:     @Column(name = "created_at", nullable = false)
24:     private Instant createdAt;
25: 
26:     @NotNull
27:     @Column(name = "llm_response", nullable = false)
28:     private String llmResponse;
29: 
30:     public Long getId() {
31:         return id;
32:     }
33: 
34:     public void setId(Long id) {
35:         this.id = id;
36:     }
37: 
38:     public PromptEntity getPrompt() {
39:         return prompt;
40:     }
41: 
42:     public void setPrompt(PromptEntity prompt) {
43:         this.prompt = prompt;
44:     }
45: 
46:     public Instant getCreatedAt() {
47:         return createdAt;
48:     }
49: 
50:     public void setCreatedAt(Instant createdAt) {
51:         this.createdAt = createdAt;
52:     }
53: 
54:     public String getLlmResponse() {
55:         return llmResponse;
56:     }
57: 
58:     public void setLlmResponse(String llmResponse) {
59:         this.llmResponse = llmResponse;
60:     }
61: 
62:     @PrePersist
63:     void onCreate() {
64:         Instant now = Instant.now();
65:         if (createdAt == null) {
66:             createdAt = now;
67:         }
68:     }
69: }

(End of file - total 69 lines)
</content>
```

---

## Assistant (Build · GPT-5.5 · 10.0s)

_Thinking:_

**Exploring code for prompt mapping**

I need to figure out if the mapper returns the Prompt contract, so I should look for the mapper first. It seems like I might need to use a code search through the indexer instead of grep. Maybe using glob for files could work? Also, I should note that the method should return List<PromptEntity> if the mapper exists. I should check if the listAllSubmittedPrompts method returns the Prompt contract and look for the mapper in service/controllers.

**Tool: task**

**Input:**
```json
{
  "description": "Find prompt mapping",
  "prompt": "In /Users/alessandrojcm/repos/prompt-vault, find Java code constructs that map PromptEntity instances to com.promptvault.contract.model.Prompt objects, especially methods called from PromptsService or controllers. Return file paths, method names, and concise snippets/line numbers. Do not modify files.",
  "subagent_type": "indexer",
  "task_id": "",
  "command": "structural search for PromptEntity to Prompt mapping"
}
```

**Output:**
```
<task id="ses_0f1beca3effeMAFovhXSYXAXDJ" state="completed">
<task_result>

</task_result>
</task>
```

---

## Assistant (Build · GPT-5.5 · 3.6s)

_Thinking:_



**Tool: glob**

**Input:**
```json
{
  "pattern": "apps/api/src/main/java/com/promptvault/api/prompt/*Controller.java",
  "path": "/Users/alessandrojcm/repos/prompt-vault"
}
```

**Output:**
```
/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptsController.java
```

---

## Assistant (Build · GPT-5.5 · 3.2s)

**Tool: read**

**Input:**
```json
{
  "filePath": "/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptsController.java",
  "offset": 1,
  "limit": 240
}
```

**Output:**
```
<path>/Users/alessandrojcm/repos/prompt-vault/apps/api/src/main/java/com/promptvault/api/prompt/PromptsController.java</path>
<type>file</type>
<content>
1: package com.promptvault.api.prompt;
2: 
3: import com.promptvault.api.auth.PromptVaultUserDetails;
4: import com.promptvault.api.user.UserEntity;
5: import com.promptvault.contract.api.PromptsApi;
6: import com.promptvault.contract.model.*;
7: import org.springframework.http.HttpStatus;
8: import org.springframework.http.ResponseEntity;
9: import org.springframework.security.core.Authentication;
10: import org.springframework.security.core.context.SecurityContextHolder;
11: import org.springframework.web.bind.annotation.RestController;
12: 
13: import java.util.List;
14: 
15: @RestController
16: public class PromptsController implements PromptsApi {
17: 
18:     private final PromptsService promptsService;
19: 
20:     public PromptsController(PromptsService promptsService) {
21:         this.promptsService = promptsService;
22:     }
23: 
24:     @Override
25:     public ResponseEntity<Prompt> createPrompt(CreatePromptRequest createPromptRequest) {
26:         Prompt prompt = PromptMapper.toContract(promptsService.createPrompt(createPromptRequest, currentUser()));
27:         return ResponseEntity.status(HttpStatus.CREATED).body(prompt);
28:     }
29: 
30:     @Override
31:     public ResponseEntity<List<Prompt>> listMyPrompts(Long userId) {
32:         UserEntity currentUser = currentUser();
33:         if (!currentUser.getId().equals(userId)) {
34:             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
35:         }
36: 
37:         return ResponseEntity.ok(promptsService.listMyPrompts(currentUser)
38:                 .stream()
39:                 .map(PromptMapper::toContract)
40:                 .toList());
41:     }
42: 
43:     @Override
44:     public ResponseEntity<Prompt> getPrompt(Long promptId) {
45:         return ResponseEntity.ok(PromptMapper.toContract(promptsService.getOwnedPrompt(promptId, currentUser())));
46:     }
47: 
48:     @Override
49:     public ResponseEntity<List<PublicPrompt>> listPublicPrompts() {
50:         return ResponseEntity.ok(promptsService.listPublicPrompts(currentUser())
51:                 .stream()
52:                 .map(PromptMapper::toPublicContract)
53:                 .toList());
54:     }
55: 
56:     @Override
57:     public ResponseEntity<PublicPrompt> getPublicPrompt(Long promptId) {
58:         return ResponseEntity.ok(PromptMapper.toPublicContract(promptsService.getPublicPrompt(promptId, currentUser())));
59:     }
60: 
61:     @Override
62:     public ResponseEntity<Prompt> updatePrompt(Long promptId, UpdatePromptRequest updatePromptRequest) {
63:         return ResponseEntity.ok(PromptMapper.toContract(promptsService.updateOwnedPrompt(
64:                 promptId,
65:                 updatePromptRequest,
66:                 currentUser()
67:         )));
68:     }
69: 
70:     @Override
71:     public ResponseEntity<Void> deletePrompt(Long promptId) {
72:         promptsService.deleteOwnedPrompt(promptId, currentUser());
73:         return ResponseEntity.noContent().build();
74:     }
75: 
76:     @Override
77:     public ResponseEntity<Prompt> updatePromptVisibility(
78:             Long promptId,
79:             UpdatePromptVisibilityRequest updatePromptVisibilityRequest
80:     ) {
81:         return ResponseEntity.ok(PromptMapper.toContract(promptsService.updateOwnedPromptVisibility(
82:                 promptId,
83:                 PromptVisibility.valueOf(updatePromptVisibilityRequest.getVisibility().getValue()),
84:                 currentUser()
85:         )));
86:     }
87: 
88:     @Override
89:     public ResponseEntity<List<Prompt>> getAllSubmissions() {
90:         return PromptsApi.super.getAllSubmissions();
91:     }
92: 
93:     @Override
94:     public ResponseEntity<List<SubmitPromptResponse>> listPromptsSubmission(Long promptId) {
95:         return ResponseEntity.ok(
96:                 promptsService.listPromptSubmissions(promptId, currentUser())
97:                         .stream()
98:                         .map(PromptSubmissionHistoryMapper::toContract)
99:                         .toList()
100:         );
101:     }
102: 
103:     @Override
104:     public ResponseEntity<SubmitPromptResponse> submitPromptRequest(Long promptId, SubmitPromptRequest submitPromptRequest) {
105:         return ResponseEntity.ok(
106:                 PromptSubmissionHistoryMapper.toContract(
107:                         promptsService.submitPrompt(promptId, submitPromptRequest, currentUser())
108:                 )
109:         );
110:     }
111: 
112:     private UserEntity currentUser() {
113:         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
114:         if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
115:             return userDetails.getUser();
116:         }
117: 
118:         throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
119:     }
120: }

(End of file - total 120 lines)
</content>
```

---

## Assistant (Build · GPT-5.5 · 35.1s)

_Thinking:_



Given your current model, “belong to the current user” means: prompts where `prompt.owner.id = currentUser.id` and the prompt has at least one `PromptSubmissionHistoryEntity`.

Add this to `PromptRepository`:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

```java
@EntityGraph(attributePaths = {"owner", "category"})
@Query("""
        select p
        from PromptEntity p
        where p.owner.id = :ownerId
          and exists (
              select 1
              from PromptSubmissionHistoryEntity h
              where h.prompt = p
          )
        order by p.createdAt desc, p.id desc
        """)
List<PromptEntity> findAllByOwnerIdWithSubmissionsOrderByCreatedAtDescIdDesc(
        @Param("ownerId") Long ownerId
);
```

Then in `PromptsService`, I’d keep the existing project pattern where services return entities and controllers map to contract DTOs:

```java
@Transactional(readOnly = true)
public List<PromptEntity> listAllSubmittedPrompts(UserEntity owner) {
    return promptRepository.findAllByOwnerIdWithSubmissionsOrderByCreatedAtDescIdDesc(owner.getId());
}
```

Then your controller can do:

```java
@Override
public ResponseEntity<List<Prompt>> getAllSubmissions() {
    return ResponseEntity.ok(
            promptsService.listAllSubmittedPrompts(currentUser())
                    .stream()
                    .map(PromptMapper::toContract)
                    .toList()
    );
}
```

You could also write it as a `join`:

```java
@Query("""
        select distinct p
        from PromptEntity p
        join p.submissions h
        where p.owner.id = :ownerId
        order by p.createdAt desc, p.id desc
        """)
```

But I’d prefer the `exists` version because it directly expresses “has at least one submission” and avoids needing `distinct` to de-duplicate prompts.

---

