package com.promptvault.api.prompt;

import java.util.List;

public class PromptValidationException extends RuntimeException {

    private final List<FieldValidationError> fieldErrors;

    public PromptValidationException(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
