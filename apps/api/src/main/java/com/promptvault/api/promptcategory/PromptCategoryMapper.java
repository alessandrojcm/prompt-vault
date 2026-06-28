package com.promptvault.api.promptcategory;

import com.promptvault.contract.model.PromptCategory;

import java.time.ZoneOffset;

public final class PromptCategoryMapper {

    private PromptCategoryMapper() {
    }

    public static PromptCategory toContract(PromptCategoryEntity category) {
        return new PromptCategory(
                category.getId(),
                category.getLabel(),
                category.getSlug(),
                category.getCreatedAt().atOffset(ZoneOffset.UTC),
                category.getCreatedBy().getId(),
                category.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
