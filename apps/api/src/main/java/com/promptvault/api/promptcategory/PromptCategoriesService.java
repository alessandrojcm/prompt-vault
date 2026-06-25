package com.promptvault.api.promptcategory;

import java.util.List;
import java.util.Locale;

import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.model.CreatePromptCategoryRequest;
import com.promptvault.contract.model.UpdatePromptCategoryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptCategoriesService {

    private final PromptCategoryRepository promptCategoryRepository;

    public PromptCategoriesService(PromptCategoryRepository promptCategoryRepository) {
        this.promptCategoryRepository = promptCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PromptCategoryEntity> listPromptCategories() {
        return promptCategoryRepository.findAllByOrderByLabelAsc();
    }

    @Transactional
    public PromptCategoryEntity createPromptCategory(CreatePromptCategoryRequest request, UserEntity creator) {
        String label = request.getLabel();
        String labelNormalized = label.toLowerCase(Locale.ROOT);
        String slug = toSnakeCaseSlug(label);

        if (promptCategoryRepository.existsByLabelNormalized(labelNormalized)) {
            throw labelValidationException("Prompt Category label must be unique.");
        }

        if (slug.isBlank() || promptCategoryRepository.existsBySlug(slug)) {
            throw labelValidationException("Prompt Category slug generated from label must be unique.");
        }

        PromptCategoryEntity category = new PromptCategoryEntity();
        category.setLabel(label);
        category.setLabelNormalized(labelNormalized);
        category.setSlug(slug);
        category.setCreatedBy(creator);

        return promptCategoryRepository.save(category);
    }

    @Transactional
    public PromptCategoryEntity updatePromptCategory(Long categoryId, UpdatePromptCategoryRequest request) {
        PromptCategoryEntity category = promptCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String label = request.getLabel();
        String labelNormalized = label.toLowerCase(Locale.ROOT);
        String slug = toSnakeCaseSlug(label);

        if (promptCategoryRepository.existsByLabelNormalizedAndIdNot(labelNormalized, categoryId)) {
            throw labelValidationException("Prompt Category label must be unique.");
        }

        if (slug.isBlank() || promptCategoryRepository.existsBySlugAndIdNot(slug, categoryId)) {
            throw labelValidationException("Prompt Category slug generated from label must be unique.");
        }

        category.setLabel(label);
        category.setLabelNormalized(labelNormalized);
        category.setSlug(slug);

        return promptCategoryRepository.save(category);
    }

    private String toSnakeCaseSlug(String label) {
        return label.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private PromptCategoryValidationException labelValidationException(String message) {
        return new PromptCategoryValidationException(List.of(new FieldValidationError("label", message)));
    }
}
