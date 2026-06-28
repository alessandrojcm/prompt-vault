package com.promptvault.api.validation;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.List;

@ControllerAdvice
public class RequestBodyNormalizationAdvice extends RequestBodyAdviceAdapter {

    private final List<RequestBodyNormalizer> normalizers;

    public RequestBodyNormalizationAdvice(List<RequestBodyNormalizer> normalizers) {
        this.normalizers = normalizers;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return normalizers.stream().anyMatch(normalizer -> normalizer.supports(methodParameter.getParameterType()));
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        Object normalizedBody = body;
        for (RequestBodyNormalizer normalizer : normalizers) {
            if (normalizer.supports(parameter.getParameterType())) {
                normalizedBody = normalizer.normalize(normalizedBody);
            }
        }

        return normalizedBody;
    }

    @Override
    public Object handleEmptyBody(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return body;
    }
}
