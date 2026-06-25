package com.promptvault.api.prompt;

import java.util.List;

import com.promptvault.api.promptcategory.PromptCategoryEntity;
import com.promptvault.api.promptcategory.PromptCategoryRepository;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.model.CreatePromptRequest;
import com.promptvault.contract.model.UpdatePromptRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PromptsService {

    private final PromptRepository promptRepository;
    private final PromptCategoryRepository promptCategoryRepository;

    public PromptsService(PromptRepository promptRepository, PromptCategoryRepository promptCategoryRepository) {
        this.promptRepository = promptRepository;
        this.promptCategoryRepository = promptCategoryRepository;
    }

    @Transactional
    public PromptEntity createPrompt(CreatePromptRequest request, UserEntity owner) {
        PromptCategoryEntity category = requireCategory(request.getCategoryId());

        PromptEntity prompt = new PromptEntity();
        prompt.setTitle(request.getTitle());
        prompt.setText(request.getText());
        prompt.setVisibility(PromptVisibility.PRIVATE);
        prompt.setOwner(owner);
        prompt.setCategory(category);

        return promptRepository.save(prompt);
    }

    @Transactional(readOnly = true)
    public List<PromptEntity> listMyPrompts(UserEntity owner) {
        return promptRepository.findAllByOwnerIdOrderByCreatedAtDescIdDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public PromptEntity getOwnedPrompt(Long promptId, UserEntity owner) {
        return requireOwnedPrompt(promptId, owner);
    }

    @Transactional
    public PromptEntity updateOwnedPrompt(Long promptId, UpdatePromptRequest request, UserEntity owner) {
        PromptEntity prompt = requireOwnedPrompt(promptId, owner);
        PromptCategoryEntity category = requireCategory(request.getCategoryId());

        prompt.setTitle(request.getTitle());
        prompt.setText(request.getText());
        prompt.setCategory(category);

        return promptRepository.save(prompt);
    }

    @Transactional
    public void deleteOwnedPrompt(Long promptId, UserEntity owner) {
        promptRepository.delete(requireOwnedPrompt(promptId, owner));
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
}
