package com.promptvault.api.prompt;

import com.promptvault.api.validation.ContractFieldValidationError;

public record FieldValidationError(String field, String message) implements ContractFieldValidationError {
}
