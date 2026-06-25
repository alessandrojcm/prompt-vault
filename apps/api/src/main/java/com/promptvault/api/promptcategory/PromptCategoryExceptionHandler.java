package com.promptvault.api.promptcategory;

import com.promptvault.api.validation.ValidationErrorResponseFactory;
import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class PromptCategoryExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public PromptCategoryExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(PromptCategoryValidationException.class)
    ResponseEntity<ValidationErrorResponse> handlePromptCategoryValidationException(PromptCategoryValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseFactory.fromDomainValidationErrors(exception.getFieldErrors()));
    }
}
