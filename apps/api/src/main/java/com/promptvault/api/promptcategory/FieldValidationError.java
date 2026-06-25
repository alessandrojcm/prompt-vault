package com.promptvault.api.promptcategory;

import com.promptvault.api.validation.ContractFieldValidationError;

public record FieldValidationError(String field, String message) implements ContractFieldValidationError {
}
