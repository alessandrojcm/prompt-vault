package com.promptvault.api.policykeyword;

import com.promptvault.api.auth.PromptVaultUserDetails;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.api.AdminPolicyKeywordsApi;
import com.promptvault.contract.model.PolicyKeyword;
import com.promptvault.contract.model.PolicyKeywordRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PolicyKeywordsController implements AdminPolicyKeywordsApi {

    private final PolicyKeywordsService policyKeywordsService;

    public PolicyKeywordsController(PolicyKeywordsService policyKeywordsService) {
        this.policyKeywordsService = policyKeywordsService;
    }

    @Override
    public ResponseEntity<List<PolicyKeyword>> listPolicyKeywords() {
        return ResponseEntity.ok(policyKeywordsService.listPolicyKeywords()
                .stream()
                .map(PolicyKeywordMapper::toContract)
                .toList());
    }

    @Override
    public ResponseEntity<PolicyKeyword> createPolicyKeyword(PolicyKeywordRequest policyKeywordRequest) {
        PolicyKeyword policyKeyword = PolicyKeywordMapper.toContract(policyKeywordsService.createPolicyKeyword(
                policyKeywordRequest,
                currentUser()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(policyKeyword);
    }

    @Override
    public ResponseEntity<PolicyKeyword> updatePolicyKeyword(Long keywordId, PolicyKeywordRequest policyKeywordRequest) {
        return ResponseEntity.ok(PolicyKeywordMapper.toContract(policyKeywordsService.updatePolicyKeyword(keywordId, policyKeywordRequest)));
    }

    @Override
    public ResponseEntity<Void> deletePolicyKeyword(Long keywordId) {
        policyKeywordsService.deletePolicyKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PromptVaultUserDetails userDetails) {
            return userDetails.getUser();
        }

        throw new IllegalStateException("Authenticated principal is not a Prompt Vault user");
    }
}
