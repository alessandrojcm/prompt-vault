package com.promptvault.api.validation;

public interface RequestBodyNormalizer {
    boolean supports(Class<?> bodyType);

    Object normalize(Object body);
}
