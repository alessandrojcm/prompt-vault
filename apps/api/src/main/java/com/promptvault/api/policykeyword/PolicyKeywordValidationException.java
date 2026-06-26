package com.promptvault.api.policykeyword;

import java.util.List;

public class PolicyKeywordValidationException extends RuntimeException {

    private final List<FieldValidationError> fieldErrors;

    public PolicyKeywordValidationException(List<FieldValidationError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
