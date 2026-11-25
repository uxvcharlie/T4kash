package com.t4kash.api.config;

import com.t4kash.api.identity.service.AuthSessionService;
import com.t4kash.api.identity.web.SessionTokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.io.IOException;

/**
 * Seguridad del API: todo exige sesion salvo la lista blanca de abajo.
 *
 * <p>Antes, una ruta era privada solo si su metodo pedia {@code @CurrentUser};
 * olvidarlo la dejaba publica. Ahora una ruta nueva nace privada y hacerla
 * publica es una decision explicita en esta lista (y en
 * ApiAuthorizationIntegrationTest, que recorre todos los endpoints).
 *
 * <p>Los permisos finos (rol de cliente o estudiante, ser duenio del recurso)
 * siguen en {@code @CurrentUser(role = ...)} y en los servicios.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /** Rutas publicas por metodo. Cada una tiene un motivo. */
    static final String[] PUBLIC_GET = {
            "/api/health",                                  // healthcheck de Render
            "/api/categories",                              // catalogo
            "/api/tasks",                                   // oportunidades (ubicacion aproximada)
            "/api/tasks/*",                                 // detalle de una oportunidad
            "/api/identity/universities",                   // formulario de registro
            "/api/identity/universities/*/careers",         // formulario de registro
            "/api/profiles/*",                              // perfil publico
            "/api/identity-verifications/callback",         // retorno del navegador desde Didit
            "/api/payments/pagadito/return",                // retorno del navegador desde Pagadito
    };
    static final String[] PUBLIC_POST = {
            "/api/auth/register",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/login",
            "/api/auth/login/verify",
            "/api/auth/login/resend",
            "/api/auth/password/forgot",
            "/api/auth/password/reset",
            "/api/identity-verifications/webhook",          // firmado por Didit
            "/api/payments/pagadito/webhook",               // firmado por Pagadito
    };
    /** Documentacion: solo existe fuera del perfil prod (application-prod la apaga). */
    static final String[] API_DOCS = {"/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthSessionService authSessionService)
            throws Exception {
        http
                // API sin cookies: el token viaja en Authorization, asi que CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'"
                        ))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(API_DOCS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        // Segunda barrera: el panel exige ADMIN aunque un metodo olvide pedirlo.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/student-verifications/pending",
                                "/api/student-verifications/*/approve",
                                "/api/student-verifications/*/reject").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            Object reason = request.getAttribute(SessionTokenAuthenticationFilter.ERROR_ATTRIBUTE);
                            writeProblem(response, HttpStatus.UNAUTHORIZED, request.getRequestURI(),
                                    reason == null ? "Debes iniciar sesion." : reason.toString());
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                writeProblem(response, HttpStatus.FORBIDDEN, request.getRequestURI(),
                                        "Tu cuenta no tiene permiso para realizar esta accion."))
                )
                .addFilterBefore(
                        new SessionTokenAuthenticationFilter(authSessionService),
                        AnonymousAuthenticationFilter.class
                );
        return http.build();
    }

    /** Mismo formato ProblemDetail que GlobalExceptionHandler. */
    private static void writeProblem(HttpServletResponse response, HttpStatus status, String path, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}"""
                .formatted(status.getReasonPhrase(), status.value(), json(detail), json(path)));
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
