package com.promptvault.api.policykeyword;

import com.promptvault.api.validation.RequestBodyNormalizer;
import com.promptvault.contract.model.PolicyKeywordRequest;
import org.springframework.stereotype.Component;

@Component
public class PolicyKeywordRequestNormalizer implements RequestBodyNormalizer {

    @Override
    public boolean supports(Class<?> bodyType) {
        return PolicyKeywordRequest.class.equals(bodyType);
    }

    @Override
    public Object normalize(Object body) {
        if (body instanceof PolicyKeywordRequest policyKeywordRequest) {
            policyKeywordRequest.setKeyword(trim(policyKeywordRequest.getKeyword()));
        }
        return body;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
