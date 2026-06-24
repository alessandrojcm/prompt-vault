package com.promptvault.api.validation;

import org.springframework.validation.FieldError;

public interface BeanValidationFieldMessageResolver {
    boolean supports(FieldError fieldError);

    String messageFor(FieldError fieldError);
}
