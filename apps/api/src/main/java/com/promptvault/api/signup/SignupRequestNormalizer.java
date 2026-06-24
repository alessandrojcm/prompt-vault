package com.promptvault.api.signup;

import com.promptvault.api.validation.RequestBodyNormalizer;
import com.promptvault.contract.model.SignupRequest;
import org.springframework.stereotype.Component;

@Component
public class SignupRequestNormalizer implements RequestBodyNormalizer {

    @Override
    public boolean supports(Class<?> bodyType) {
        return SignupRequest.class.equals(bodyType);
    }

    @Override
    public Object normalize(Object body) {
        if (body instanceof SignupRequest signupRequest) {
            signupRequest.setUsername(trim(signupRequest.getUsername()));
            signupRequest.setEmailAddress(trim(signupRequest.getEmailAddress()));
        }

        return body;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
