package com.promptvault.api.currentuser;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.UserEntity;
import com.promptvault.api.user.UserSummaryMapper;
import com.promptvault.contract.api.CurrentUserApi;
import com.promptvault.contract.model.UserSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController implements CurrentUserApi {

    @Override
    public ResponseEntity<UserSummary> getCurrentUser() {
        return ResponseEntity.ok(UserSummaryMapper.toSummary(currentUser()));
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
