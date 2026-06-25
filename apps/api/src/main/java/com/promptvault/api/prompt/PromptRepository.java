package com.promptvault.api.prompt;

import java.util.List;
import java.util.Optional;

import com.promptvault.api.user.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @EntityGraph(attributePaths = { "owner", "category" })
    List<PromptEntity> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    @EntityGraph(attributePaths = { "owner", "category" })
    Optional<PromptEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = { "owner", "category" })
    List<PromptEntity> findAllByVisibilityAndOwnerAccountStatusAndOwnerIdNotOrderByCreatedAtDescIdDesc(
        PromptVisibility visibility,
        AccountStatus ownerStatus,
        Long excludedOwnerId
    );

    @EntityGraph(attributePaths = { "owner", "category" })
    Optional<PromptEntity> findByIdAndVisibilityAndOwnerAccountStatusAndOwnerIdNot(
        Long id,
        PromptVisibility visibility,
        AccountStatus ownerStatus,
        Long excludedOwnerId
    );
}
