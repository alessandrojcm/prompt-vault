package com.promptvault.api.signup;

import java.util.List;

public class SignupValidationException extends RuntimeException {

    private final List<FieldValidationError> fieldErrors;

    public SignupValidationException(List<FieldValidationError> fieldErrors) {
        super("Signup validation failed");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
