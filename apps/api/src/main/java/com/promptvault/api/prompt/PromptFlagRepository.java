package com.promptvault.api.prompt;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptFlagRepository extends JpaRepository<PromptFlagEntity, Long> {

    @EntityGraph(attributePaths = "keywordSnapshots")
    Optional<PromptFlagEntity> findByPromptId(Long promptId);
}
