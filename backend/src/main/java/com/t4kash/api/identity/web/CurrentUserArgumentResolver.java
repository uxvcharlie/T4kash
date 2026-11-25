package com.t4kash.api.identity.web;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.service.AuthenticatedUserService;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthenticatedUserService authenticatedUserService;

    public CurrentUserArgumentResolver(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthenticatedUserResponse.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String authorization = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);
        if (!annotation.required() && (authorization == null || authorization.isBlank())) {
            return null;
        }
        String role = annotation.role();
        // El filtro de seguridad ya valido el token: se reutiliza sin consultar la base.
        Object resolved = webRequest.getAttribute(
                SessionTokenAuthenticationFilter.USER_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST
        );
        if (resolved instanceof AuthenticatedUserResponse user) {
            return role.isBlank() ? user : authenticatedUserService.requireRole(user, role);
        }
        return role.isBlank()
                ? authenticatedUserService.requireUser(authorization)
                : authenticatedUserService.requireRole(authorization, role);
    }
}
