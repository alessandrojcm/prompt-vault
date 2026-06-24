package com.promptvault.api.signup;

import java.util.List;

import com.promptvault.contract.model.ValidationErrorResponse;
import com.promptvault.contract.model.ValidationFieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SignupExceptionHandler {

    @ExceptionHandler(SignupValidationException.class)
    ResponseEntity<ValidationErrorResponse> handleSignupValidationException(SignupValidationException exception) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setMessage("Signup validation failed.");
        response.setFieldErrors(toContractErrors(exception.getFieldErrors()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private List<ValidationFieldError> toContractErrors(List<FieldValidationError> fieldErrors) {
        return fieldErrors.stream().map(fieldError -> {
            ValidationFieldError contractError = new ValidationFieldError();
            contractError.setField(fieldError.field());
            contractError.setMessage(fieldError.message());
            return contractError;
        }).toList();
    }
}
