package com.promptvault.api.promptcategory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptCategoryRepository extends JpaRepository<PromptCategoryEntity, Long> {

    List<PromptCategoryEntity> findAllByOrderByLabelAsc();
}
