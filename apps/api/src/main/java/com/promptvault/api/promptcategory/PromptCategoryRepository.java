package com.promptvault.api.promptcategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptCategoryRepository extends JpaRepository<PromptCategoryEntity, Long> {

    List<PromptCategoryEntity> findAllByOrderByLabelAsc();

    boolean existsByLabelNormalized(String labelNormalized);

    boolean existsBySlug(String slug);

    boolean existsByLabelNormalizedAndIdNot(String labelNormalized, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
