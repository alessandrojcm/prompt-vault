package com.promptvault.api.prompt;

import com.promptvault.api.validation.ValidationErrorResponseFactory;
import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class PromptExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public PromptExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(PromptValidationException.class)
    ResponseEntity<ValidationErrorResponse> handlePromptValidationException(PromptValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseFactory.fromDomainValidationErrors(exception.getFieldErrors()));
    }
}
