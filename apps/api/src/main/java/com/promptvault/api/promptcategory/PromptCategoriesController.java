package com.promptvault.api.promptcategory;

import java.util.List;

import com.promptvault.contract.api.PromptCategoriesApi;
import com.promptvault.contract.model.PromptCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromptCategoriesController implements PromptCategoriesApi {

    private final PromptCategoryRepository promptCategoryRepository;

    public PromptCategoriesController(PromptCategoryRepository promptCategoryRepository) {
        this.promptCategoryRepository = promptCategoryRepository;
    }

    @Override
    public ResponseEntity<List<PromptCategory>> listPromptCategories() {
        return ResponseEntity.ok(promptCategoryRepository.findAllByOrderByLabelAsc()
            .stream()
            .map(PromptCategoryMapper::toContract)
            .toList());
    }
}
