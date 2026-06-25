package com.promptvault.api.prompt;

import com.promptvault.api.validation.RequestBodyNormalizer;
import com.promptvault.contract.model.CreatePromptRequest;
import com.promptvault.contract.model.UpdatePromptRequest;
import org.springframework.stereotype.Component;

@Component
public class CreatePromptRequestNormalizer implements RequestBodyNormalizer {

    @Override
    public boolean supports(Class<?> bodyType) {
        return CreatePromptRequest.class.equals(bodyType) || UpdatePromptRequest.class.equals(bodyType);
    }

    @Override
    public Object normalize(Object body) {
        if (body instanceof CreatePromptRequest createPromptRequest) {
            createPromptRequest.setTitle(trim(createPromptRequest.getTitle()));
            createPromptRequest.setText(trim(createPromptRequest.getText()));
        }
        if (body instanceof UpdatePromptRequest updatePromptRequest) {
            updatePromptRequest.setTitle(trim(updatePromptRequest.getTitle()));
            updatePromptRequest.setText(trim(updatePromptRequest.getText()));
        }

        return body;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
