package com.promptvault.api.policykeyword;

import java.time.ZoneOffset;

import com.promptvault.contract.model.PolicyKeyword;

public final class PolicyKeywordMapper {

    private PolicyKeywordMapper() {
    }

    public static PolicyKeyword toContract(PolicyKeywordEntity policyKeyword) {
        return new PolicyKeyword(
            policyKeyword.getId(),
            policyKeyword.getKeyword(),
            policyKeyword.getCreatedAt().atOffset(ZoneOffset.UTC),
            policyKeyword.getCreatedBy().getId(),
            policyKeyword.getCreatedBy().getUsername(),
            policyKeyword.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
