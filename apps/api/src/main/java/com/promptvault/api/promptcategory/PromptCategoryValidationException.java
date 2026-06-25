package com.promptvault.api.promptcategory;

import java.util.List;

public class PromptCategoryValidationException extends RuntimeException {

    private final List<FieldValidationError> fieldErrors;

    public PromptCategoryValidationException(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
