package com.promptvault.api.prompt;

import java.util.List;
import java.util.Optional;

import com.promptvault.api.user.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @EntityGraph(attributePaths = { "owner", "category", "flag" })
    List<PromptEntity> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    @EntityGraph(attributePaths = { "owner", "category", "flag" })
    Optional<PromptEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = { "owner", "category", "flag.keywordSnapshots" })
    List<PromptEntity> findAllByFlagIsNotNullOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = { "owner", "category" })
    List<PromptEntity> findAllByVisibilityAndFlagIsNullAndOwnerAccountStatusAndOwnerIdNotOrderByCreatedAtDescIdDesc(
        PromptVisibility visibility,
        AccountStatus ownerStatus,
        Long excludedOwnerId
    );

    @EntityGraph(attributePaths = { "owner", "category" })
    Optional<PromptEntity> findByIdAndVisibilityAndFlagIsNullAndOwnerAccountStatusAndOwnerIdNot(
        Long id,
        PromptVisibility visibility,
        AccountStatus ownerStatus,
        Long excludedOwnerId
    );

    boolean existsByCategoryId(Long categoryId);
}
