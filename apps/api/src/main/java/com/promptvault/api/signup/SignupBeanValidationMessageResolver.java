package com.promptvault.api.signup;

import com.promptvault.api.validation.BeanValidationFieldMessageResolver;
import com.promptvault.contract.model.SignupRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

@Component
public class SignupBeanValidationMessageResolver implements BeanValidationFieldMessageResolver {

    @Override
    public boolean supports(FieldError fieldError) {
        return SignupRequest.class.getSimpleName().equalsIgnoreCase(fieldError.getObjectName())
                || "signupRequest".equals(fieldError.getObjectName());
    }

    @Override
    public String messageFor(FieldError fieldError) {
        return switch (fieldError.getField()) {
            case "username" -> "Username must be 3 to 30 characters long.";
            case "emailAddress" -> "Email Address must be valid.";
            case "password" -> "Password must be at least 8 characters long.";
            default -> fieldError.getDefaultMessage();
        };
    }
}
