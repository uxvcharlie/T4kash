package com.t4kash.api.identity.web;

import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.service.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida el token de sesion una sola vez por peticion.
 *
 * <p>Si el token es valido, deja la autenticacion en el contexto de Spring
 * Security (con los roles como ROLE_*) y el usuario en un atributo de la
 * peticion, que {@link CurrentUserArgumentResolver} reutiliza sin volver a
 * consultar la base. Si es invalido, no autentica y guarda el motivo para la
 * respuesta 401.
 */
public class SessionTokenAuthenticationFilter extends OncePerRequestFilter {
    public static final String USER_ATTRIBUTE = SessionTokenAuthenticationFilter.class.getName() + ".USER";
    public static final String ERROR_ATTRIBUTE = SessionTokenAuthenticationFilter.class.getName() + ".ERROR";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    public SessionTokenAuthenticationFilter(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null
                && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                authenticate(request, token);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AuthenticatedUserResponse user = authSessionService.getCurrentUser(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .toList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(USER_ATTRIBUTE, user);
        } catch (InvalidCredentialsException exception) {
            request.setAttribute(ERROR_ATTRIBUTE, exception.getMessage());
        }
    }
}
