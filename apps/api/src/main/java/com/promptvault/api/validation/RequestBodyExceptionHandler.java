package com.promptvault.api.validation;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.promptvault.contract.model.ValidationErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@ConditionalOnBean(ValidationErrorResponseFactory.class)
public class RequestBodyExceptionHandler {

    private final ValidationErrorResponseFactory responseFactory;

    public RequestBodyExceptionHandler(ValidationErrorResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ValidationErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        RequestBodyValidationError validationError = validationErrorFor(exception);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(responseFactory.fromDomainValidationErrors(List.of(validationError)));
    }

    private RequestBodyValidationError validationErrorFor(HttpMessageNotReadableException exception) {
        InvalidFormatException invalidFormatException = invalidFormatExceptionFrom(exception);
        if (invalidFormatException != null
                && invalidFormatException.getTargetType().isEnum()
                && "AccountStatus".equals(invalidFormatException.getTargetType().getSimpleName())) {
            return new RequestBodyValidationError(fieldName(invalidFormatException), "Account status must be ENABLED or DISABLED.");
        }

        if (containsMessage(exception, "accountStatus")) {
            return new RequestBodyValidationError("accountStatus", "Account status must be ENABLED or DISABLED.");
        }

        return new RequestBodyValidationError("body", "Request body is invalid.");
    }

    private InvalidFormatException invalidFormatExceptionFrom(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean containsMessage(Throwable throwable, String value) {
        Throwable current = throwable;
        while (current != null) {
            if (String.valueOf(current.getMessage()).contains(value)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String fieldName(InvalidFormatException exception) {
        return exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                .reduce((ignored, fieldName) -> fieldName)
                .orElse("body");
    }

    private record RequestBodyValidationError(String field, String message) implements ContractFieldValidationError {
    }
}
