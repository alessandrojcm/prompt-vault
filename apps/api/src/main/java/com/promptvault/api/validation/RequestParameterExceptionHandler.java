package com.promptvault.api.validation;

import java.util.List;

import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class RequestParameterExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public RequestParameterExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ValidationErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(responseFactory.fromDomainValidationErrors(List.of(
                new RequestParameterValidationError(exception.getName(), "Role must be USER or ADMIN.")
            )));
    }

    private record RequestParameterValidationError(String field, String message) implements ContractFieldValidationError {
    }
}
