package com.promptvault.api.policykeyword;

import com.promptvault.api.validation.ValidationErrorResponseFactory;
import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class PolicyKeywordExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public PolicyKeywordExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(PolicyKeywordValidationException.class)
    ResponseEntity<ValidationErrorResponse> handlePolicyKeywordValidationException(PolicyKeywordValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseFactory.fromDomainValidationErrors(exception.getFieldErrors()));
    }
}
