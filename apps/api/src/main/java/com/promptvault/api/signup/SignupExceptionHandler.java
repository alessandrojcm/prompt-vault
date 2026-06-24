package com.promptvault.api.signup;

import com.promptvault.api.validation.ValidationErrorResponseFactory;
import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class SignupExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public SignupExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(SignupValidationException.class)
    ResponseEntity<ValidationErrorResponse> handleSignupValidationException(SignupValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseFactory.fromDomainValidationErrors(exception.getFieldErrors()));
    }
}
