package com.promptvault.api.promptcategory;

import java.util.List;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.api.PromptCategoriesApi;
import com.promptvault.contract.model.CreatePromptCategoryRequest;
import com.promptvault.contract.model.PromptCategory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromptCategoriesController implements PromptCategoriesApi {

    private final PromptCategoriesService promptCategoriesService;

    public PromptCategoriesController(PromptCategoriesService promptCategoriesService) {
        this.promptCategoriesService = promptCategoriesService;
    }

    @Override
    public ResponseEntity<List<PromptCategory>> listPromptCategories() {
        return ResponseEntity.ok(promptCategoriesService.listPromptCategories()
            .stream()
            .map(PromptCategoryMapper::toContract)
            .toList());
    }

    @Override
    public ResponseEntity<PromptCategory> createPromptCategory(CreatePromptCategoryRequest createPromptCategoryRequest) {
        PromptCategory category = PromptCategoryMapper.toContract(promptCategoriesService.createPromptCategory(
            createPromptCategoryRequest,
            currentUser()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
