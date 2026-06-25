package com.promptvault.api.prompt;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    @EntityGraph(attributePaths = { "owner", "category" })
    List<PromptEntity> findAllByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);
}
