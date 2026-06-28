package com.promptvault.api.prompt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromptSubmissionHistoryRepository extends JpaRepository<PromptSubmissionHistoryEntity, Long> {
    
    List<PromptSubmissionHistoryEntity> findAllByPromptIdAndPromptOwnerIdOrderByCreatedAtDescIdDesc(
            Long promptId,
            Long ownerId
    );
}
