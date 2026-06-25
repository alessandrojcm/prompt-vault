package com.promptvault.api.prompt;

import java.util.List;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.api.PromptsApi;
import com.promptvault.contract.model.CreatePromptRequest;
import com.promptvault.contract.model.Prompt;
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

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
