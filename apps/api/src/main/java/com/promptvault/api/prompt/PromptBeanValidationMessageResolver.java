package com.promptvault.api.prompt;

import com.promptvault.api.validation.BeanValidationFieldMessageResolver;
import com.promptvault.contract.model.CreatePromptRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

@Component
public class PromptBeanValidationMessageResolver implements BeanValidationFieldMessageResolver {

    @Override
    public boolean supports(FieldError fieldError) {
        return CreatePromptRequest.class.getSimpleName().equalsIgnoreCase(fieldError.getObjectName())
            || "createPromptRequest".equals(fieldError.getObjectName());
    }

    @Override
    public String messageFor(FieldError fieldError) {
        return switch (fieldError.getField()) {
            case "title" -> "Prompt Title must be 1 to 120 characters long.";
            case "text" -> "Prompt Text must be 1 to 10,000 characters long.";
            case "categoryId" -> "Prompt Category is required.";
            default -> fieldError.getDefaultMessage();
        };
    }
}
