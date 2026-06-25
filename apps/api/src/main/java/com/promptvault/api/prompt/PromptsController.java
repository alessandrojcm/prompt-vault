package com.promptvault.api.prompt;

import java.util.List;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.api.PromptsApi;
import com.promptvault.contract.model.CreatePromptRequest;
import com.promptvault.contract.model.Prompt;
import com.promptvault.contract.model.UpdatePromptRequest;
import com.promptvault.contract.model.UpdatePromptVisibilityRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromptsController implements PromptsApi {

    private final PromptsService promptsService;

    public PromptsController(PromptsService promptsService) {
        this.promptsService = promptsService;
    }

    @Override
    public ResponseEntity<Prompt> createPrompt(CreatePromptRequest createPromptRequest) {
        Prompt prompt = PromptMapper.toContract(promptsService.createPrompt(createPromptRequest, currentUser()));
        return ResponseEntity.status(HttpStatus.CREATED).body(prompt);
    }

    @Override
    public ResponseEntity<List<Prompt>> listMyPrompts(Long userId) {
        UserEntity currentUser = currentUser();
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(promptsService.listMyPrompts(currentUser)
            .stream()
            .map(PromptMapper::toContract)
            .toList());
    }

    @Override
    public ResponseEntity<Prompt> getPrompt(Long promptId) {
        return ResponseEntity.ok(PromptMapper.toContract(promptsService.getOwnedPrompt(promptId, currentUser())));
    }

    @Override
    public ResponseEntity<Prompt> updatePrompt(Long promptId, UpdatePromptRequest updatePromptRequest) {
        return ResponseEntity.ok(PromptMapper.toContract(promptsService.updateOwnedPrompt(
            promptId,
            updatePromptRequest,
            currentUser()
        )));
    }

    @Override
    public ResponseEntity<Void> deletePrompt(Long promptId) {
        promptsService.deleteOwnedPrompt(promptId, currentUser());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Prompt> updatePromptVisibility(
        Long promptId,
        UpdatePromptVisibilityRequest updatePromptVisibilityRequest
    ) {
        return ResponseEntity.ok(PromptMapper.toContract(promptsService.updateOwnedPromptVisibility(
            promptId,
            PromptVisibility.valueOf(updatePromptVisibilityRequest.getVisibility().getValue()),
            currentUser()
        )));
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
