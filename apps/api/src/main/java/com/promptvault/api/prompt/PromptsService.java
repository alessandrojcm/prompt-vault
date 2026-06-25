package com.promptvault.api.prompt;

import java.util.List;

import com.promptvault.api.promptcategory.PromptCategoryEntity;
import com.promptvault.api.promptcategory.PromptCategoryRepository;
import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.model.CreatePromptRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        PromptCategoryEntity category = promptCategoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new PromptValidationException(List.of(new FieldValidationError(
                "categoryId",
                "Prompt Category must exist."
            ))));

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
}
