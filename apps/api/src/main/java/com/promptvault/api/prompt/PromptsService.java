package com.promptvault.api.prompt;

import com.promptvault.api.policykeyword.PolicyKeywordEntity;
import com.promptvault.api.policykeyword.PolicyKeywordRepository;
import com.promptvault.api.promptcategory.PromptCategoryEntity;
import com.promptvault.api.promptcategory.PromptCategoryRepository;
import com.promptvault.api.user.AccountStatus;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.model.CreatePromptRequest;
import com.promptvault.contract.model.SubmitPromptRequest;
import com.promptvault.contract.model.UpdatePromptRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PromptsService {

    private final PromptRepository promptRepository;
    private final PromptCategoryRepository promptCategoryRepository;
    private final PolicyKeywordRepository policyKeywordRepository;
    private final PromptSubmissionHistoryRepository promptSubmissionHistoryRepository;

    public PromptsService(
            PromptRepository promptRepository,
            PromptCategoryRepository promptCategoryRepository,
            PolicyKeywordRepository policyKeywordRepository,
            PromptSubmissionHistoryRepository promptSubmissionHistoryRepository
    ) {
        this.promptRepository = promptRepository;
        this.promptCategoryRepository = promptCategoryRepository;
        this.policyKeywordRepository = policyKeywordRepository;
        this.promptSubmissionHistoryRepository = promptSubmissionHistoryRepository;
    }

    @Transactional
    public PromptEntity createPrompt(CreatePromptRequest request, UserEntity owner) {
        PromptCategoryEntity category = requireCategory(request.getCategoryId());

        PromptEntity prompt = new PromptEntity();
        prompt.setTitle(request.getTitle());
        prompt.setText(request.getText());
        prompt.setVisibility(PromptVisibility.valueOf(request.getVisibility().getValue()));
        prompt.setOwner(owner);
        prompt.setCategory(category);
        attachPromptFlagForMatchingPolicyKeywords(prompt);

        return promptRepository.save(prompt);
    }

    @Transactional(readOnly = true)
    public List<PromptEntity> listMyPrompts(UserEntity owner) {
        return promptRepository.findAllByOwnerIdOrderByCreatedAtDescIdDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public List<PromptEntity> listVisiblePrompts(
            UserEntity currentUser,
            Set<com.promptvault.contract.model.PromptVisibility> visibilityFilter
    ) {
        Set<PromptVisibility> requestedVisibilities = requestedVisibilities(visibilityFilter);
        return promptRepository.findVisiblePromptsOrderByCreatedAtDescIdDesc(
                currentUser.getId(),
                requestedVisibilities.contains(PromptVisibility.PUBLIC),
                requestedVisibilities.contains(PromptVisibility.PRIVATE),
                PromptVisibility.PUBLIC,
                PromptVisibility.PRIVATE,
                AccountStatus.ENABLED
        );
    }

    @Transactional(readOnly = true)
    public PromptEntity getVisiblePrompt(Long promptId, UserEntity currentUser) {
        PromptEntity prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (isPubliclyVisible(prompt) || prompt.getOwner().getId().equals(currentUser.getId())) {
            return prompt;
        }

        throw new ResponseStatusException(FORBIDDEN);
    }

    private Set<PromptVisibility> requestedVisibilities(
            Set<com.promptvault.contract.model.PromptVisibility> visibilityFilter
    ) {
        if (visibilityFilter == null || visibilityFilter.isEmpty()) {
            return Set.of(PromptVisibility.PUBLIC, PromptVisibility.PRIVATE);
        }

        return visibilityFilter.stream()
                .map(visibility -> PromptVisibility.valueOf(visibility.getValue()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isPubliclyVisible(PromptEntity prompt) {
        return prompt.getVisibility() == PromptVisibility.PUBLIC
                && prompt.getFlag() == null
                && prompt.getOwner().getAccountStatus() == AccountStatus.ENABLED;
    }

    @Transactional
    public PromptEntity updateOwnedPrompt(Long promptId, UpdatePromptRequest request, UserEntity owner) {
        PromptEntity prompt = requireOwnedPrompt(promptId, owner);
        PromptCategoryEntity category = requireCategory(request.getCategoryId());
        boolean textChanged = !prompt.getText().equals(request.getText());

        prompt.setTitle(request.getTitle());
        prompt.setText(request.getText());
        prompt.setCategory(category);
        if (textChanged) {
            refreshPromptFlagForCurrentText(prompt);
            if (prompt.getFlag() != null) {
                prompt.setVisibility(PromptVisibility.PRIVATE);
            }
        }

        return promptRepository.save(prompt);
    }

    @Transactional
    public void deleteOwnedPrompt(Long promptId, UserEntity owner) {
        promptRepository.delete(requireOwnedPrompt(promptId, owner));
    }

    @Transactional
    public PromptEntity updateOwnedPromptVisibility(Long promptId, PromptVisibility visibility, UserEntity owner) {
        PromptEntity prompt = requireOwnedPrompt(promptId, owner);
        if (visibility == PromptVisibility.PUBLIC && prompt.getFlag() != null) {
            throw new PromptValidationException(List.of(new FieldValidationError(
                    "visibility",
                    "Flagged Prompts cannot be public."
            )));
        }
        prompt.setVisibility(visibility);
        return promptRepository.save(prompt);
    }

    @Transactional
    public PromptSubmissionHistoryEntity submitPrompt(Long promptId, SubmitPromptRequest request, UserEntity owner) {
        PromptEntity prompt = getVisiblePrompt(promptId, owner);
        PromptSubmissionHistoryEntity submission = new PromptSubmissionHistoryEntity();
        submission.setLlmResponse(request.getResponse());
        prompt.setSubmissions(submission);
        submission.setPrompt(prompt);
        promptRepository.save(prompt);

        return prompt.getSubmissions().getLast();
    }

    @Transactional(readOnly = true)
    public List<PromptSubmissionHistoryEntity> listPromptSubmissions(Long promptId, UserEntity owner) {
        return promptSubmissionHistoryRepository.findAllByPromptIdAndPromptOwnerIdOrderByCreatedAtDescIdDesc(promptId, owner.getId());
    }

    @Transactional(readOnly = true)
    public List<PromptEntity> listAllSubmittedPrompts(UserEntity owner) {
        return promptRepository.findAllByOwnerIdWithSubmissionsOrderByCreatedAtDescIdDesc(owner.getId());
    }

    private PromptEntity requireOwnedPrompt(Long promptId, UserEntity owner) {
        return promptRepository.findByIdAndOwnerId(promptId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private PromptCategoryEntity requireCategory(Long categoryId) {
        return promptCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new PromptValidationException(List.of(new FieldValidationError(
                        "categoryId",
                        "Prompt Category must exist."
                ))));
    }

    private void attachPromptFlagForMatchingPolicyKeywords(PromptEntity prompt) {
        List<String> matchedKeywords = matchingPolicyKeywords(prompt);

        if (matchedKeywords.isEmpty()) {
            return;
        }

        PromptFlagEntity flag = new PromptFlagEntity();
        matchedKeywords.forEach(flag::addKeywordSnapshot);
        prompt.setFlag(flag);
    }

    private void refreshPromptFlagForCurrentText(PromptEntity prompt) {
        List<String> matchedKeywords = matchingPolicyKeywords(prompt);

        if (matchedKeywords.isEmpty()) {
            prompt.setFlag(null);
            return;
        }

        if (prompt.getFlag() == null) {
            attachPromptFlagForMatchingPolicyKeywords(prompt);
            return;
        }

        prompt.getFlag().replaceKeywordSnapshots(matchedKeywords);
    }

    private List<String> matchingPolicyKeywords(PromptEntity prompt) {
        String normalizedPromptText = prompt.getText().toLowerCase(Locale.ROOT);
        return policyKeywordRepository.findAllByOrderByKeywordAsc()
                .stream()
                .map(PolicyKeywordEntity::getKeyword)
                .filter(keyword -> normalizedPromptText.contains(keyword.toLowerCase(Locale.ROOT)))
                .toList();
    }
}
