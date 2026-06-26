package com.promptvault.api.policykeyword;

import java.util.List;
import java.util.Locale;

import com.promptvault.api.user.UserEntity;
import com.promptvault.contract.model.PolicyKeywordRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyKeywordsService {

    private final PolicyKeywordRepository policyKeywordRepository;

    public PolicyKeywordsService(PolicyKeywordRepository policyKeywordRepository) {
        this.policyKeywordRepository = policyKeywordRepository;
    }

    @Transactional(readOnly = true)
    public List<PolicyKeywordEntity> listPolicyKeywords() {
        return policyKeywordRepository.findAllByOrderByKeywordAsc();
    }

    @Transactional
    public PolicyKeywordEntity createPolicyKeyword(PolicyKeywordRequest request, UserEntity creator) {
        String keyword = request.getKeyword();
        String keywordNormalized = normalizeKeyword(keyword);
        if (policyKeywordRepository.existsByKeywordNormalized(keywordNormalized)) {
            throw keywordValidationException();
        }
        PolicyKeywordEntity policyKeyword = new PolicyKeywordEntity();
        policyKeyword.setKeyword(keyword);
        policyKeyword.setKeywordNormalized(keywordNormalized);
        policyKeyword.setCreatedBy(creator);
        return policyKeywordRepository.save(policyKeyword);
    }

    @Transactional
    public PolicyKeywordEntity updatePolicyKeyword(Long keywordId, PolicyKeywordRequest request) {
        PolicyKeywordEntity policyKeyword = policyKeywordRepository.findById(keywordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String keyword = request.getKeyword();
        String keywordNormalized = normalizeKeyword(keyword);
        if (policyKeywordRepository.existsByKeywordNormalizedAndIdNot(keywordNormalized, keywordId)) {
            throw keywordValidationException();
        }
        policyKeyword.setKeyword(keyword);
        policyKeyword.setKeywordNormalized(keywordNormalized);
        return policyKeywordRepository.save(policyKeyword);
    }

    @Transactional
    public void deletePolicyKeyword(Long keywordId) {
        PolicyKeywordEntity policyKeyword = policyKeywordRepository.findById(keywordId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        policyKeywordRepository.delete(policyKeyword);
    }

    private String normalizeKeyword(String keyword) {
        return keyword.toLowerCase(Locale.ROOT);
    }

    private PolicyKeywordValidationException keywordValidationException() {
        return new PolicyKeywordValidationException(List.of(new FieldValidationError("keyword", "Policy Keyword must be unique.")));
    }
}
