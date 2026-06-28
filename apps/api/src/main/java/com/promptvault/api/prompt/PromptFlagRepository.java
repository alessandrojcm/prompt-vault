package com.promptvault.api.prompt;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptFlagRepository extends JpaRepository<PromptFlagEntity, Long> {

    @EntityGraph(attributePaths = "keywordSnapshots")
    Optional<PromptFlagEntity> findByPromptId(Long promptId);
}
