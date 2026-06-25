package com.promptvault.api.promptcategory;

import com.promptvault.api.validation.RequestBodyNormalizer;
import com.promptvault.contract.model.CreatePromptCategoryRequest;
import org.springframework.stereotype.Component;

@Component
public class CreatePromptCategoryRequestNormalizer implements RequestBodyNormalizer {

    @Override
    public boolean supports(Class<?> bodyType) {
        return CreatePromptCategoryRequest.class.equals(bodyType);
    }

    @Override
    public Object normalize(Object body) {
        if (body instanceof CreatePromptCategoryRequest createPromptCategoryRequest) {
            createPromptCategoryRequest.setLabel(trim(createPromptCategoryRequest.getLabel()));
        }

        return body;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
