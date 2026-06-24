package com.promptvault.api.signup;

import com.promptvault.api.validation.ContractFieldValidationError;

record FieldValidationError(String field, String message) implements ContractFieldValidationError {
}
