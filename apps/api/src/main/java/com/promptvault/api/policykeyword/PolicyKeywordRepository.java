package com.promptvault.api.policykeyword;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyKeywordRepository extends JpaRepository<PolicyKeywordEntity, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<PolicyKeywordEntity> findAllByOrderByKeywordAsc();

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Optional<PolicyKeywordEntity> findById(Long id);

    boolean existsByKeywordNormalized(String keywordNormalized);

    boolean existsByKeywordNormalizedAndIdNot(String keywordNormalized, Long id);
}
