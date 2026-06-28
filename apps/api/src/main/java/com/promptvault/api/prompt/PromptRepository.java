package com.promptvault.api.prompt;

import com.promptvault.api.user.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    Optional<PromptEntity> findById(Long id);

    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    List<PromptEntity> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    Optional<PromptEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"owner", "category", "flag"})
    @Query("""
            select p
            from PromptEntity p
            where (
                    :includePublic = true
                    and p.visibility = :publicVisibility
                    and p.flag is null
                    and p.owner.accountStatus = :enabledStatus
                  )
               or (
                    :includePrivate = true
                    and p.visibility = :privateVisibility
                    and p.owner.id = :ownerId
                  )
            order by p.createdAt desc, p.id desc
            """)
    List<PromptEntity> findVisiblePromptsOrderByCreatedAtDescIdDesc(
            @Param("ownerId") Long ownerId,
            @Param("includePublic") boolean includePublic,
            @Param("includePrivate") boolean includePrivate,
            @Param("publicVisibility") PromptVisibility publicVisibility,
            @Param("privateVisibility") PromptVisibility privateVisibility,
            @Param("enabledStatus") AccountStatus enabledStatus
    );

    @EntityGraph(attributePaths = {"owner", "category", "flag.keywordSnapshots"})
    List<PromptEntity> findAllByFlagIsNotNullOrderByCreatedAtDescIdDesc();

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
