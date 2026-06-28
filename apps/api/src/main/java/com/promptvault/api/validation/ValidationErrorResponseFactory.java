package com.promptvault.api.validation;

import com.promptvault.contract.model.ValidationErrorResponse;
import com.promptvault.contract.model.ValidationFieldError;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.List;

@Component
public class ValidationErrorResponseFactory {

    public static final String VALIDATION_FAILED_MESSAGE = "Signup validation failed.";

    private final List<BeanValidationFieldMessageResolver> messageResolvers;

    public ValidationErrorResponseFactory(List<BeanValidationFieldMessageResolver> messageResolvers) {
        this.messageResolvers = messageResolvers;
    }

    public ValidationErrorResponse fromBeanValidationErrors(List<FieldError> fieldErrors) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setMessage(VALIDATION_FAILED_MESSAGE);
        response.setFieldErrors(fieldErrors.stream().map(this::toContractError).toList());
        return response;
    }

    public ValidationErrorResponse fromDomainValidationErrors(List<? extends ContractFieldValidationError> fieldErrors) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setMessage(VALIDATION_FAILED_MESSAGE);
        response.setFieldErrors(fieldErrors.stream().map(this::toContractError).toList());
        return response;
    }

    private ValidationFieldError toContractError(FieldError fieldError) {
        ValidationFieldError contractError = new ValidationFieldError();
        contractError.setField(fieldError.getField());
        contractError.setMessage(messageFor(fieldError));
        return contractError;
    }

    private ValidationFieldError toContractError(ContractFieldValidationError fieldError) {
        ValidationFieldError contractError = new ValidationFieldError();
        contractError.setField(fieldError.field());
        contractError.setMessage(fieldError.message());
        return contractError;
    }

    private String messageFor(FieldError fieldError) {
        return messageResolvers.stream()
                .filter(resolver -> resolver.supports(fieldError))
                .findFirst()
                .map(resolver -> resolver.messageFor(fieldError))
                .orElseGet(fieldError::getDefaultMessage);
    }
}
