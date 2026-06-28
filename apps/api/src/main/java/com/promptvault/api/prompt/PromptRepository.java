package com.promptvault.api.prompt;

import com.promptvault.api.user.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    List<PromptEntity> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    Optional<PromptEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"owner", "category", "flag.keywordSnapshots"})
    List<PromptEntity> findAllByFlagIsNotNullOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"owner", "category"})
    List<PromptEntity> findAllByVisibilityAndFlagIsNullAndOwnerAccountStatusAndOwnerIdNotOrderByCreatedAtDescIdDesc(
            PromptVisibility visibility,
            AccountStatus ownerStatus,
            Long excludedOwnerId
    );

    @EntityGraph(attributePaths = {"owner", "category"})
    Optional<PromptEntity> findByIdAndVisibilityAndFlagIsNullAndOwnerAccountStatusAndOwnerIdNot(
            Long id,
            PromptVisibility visibility,
            AccountStatus ownerStatus,
            Long excludedOwnerId
    );

    @EntityGraph(attributePaths = {"owner", "category"})
    @Query("""
            select p
            from PromptEntity p
            where p.owner.id = :ownerId
              and exists (
                  select 1
                  from PromptSubmissionHistoryEntity h
                  where h.prompt = p
              )
            order by p.createdAt desc, p.id desc
            """)
    List<PromptEntity> findAllByOwnerIdWithSubmissionsOrderByCreatedAtDescIdDesc(
            @Param("ownerId") Long ownerId
    );

    boolean existsByCategoryId(Long categoryId);
}
