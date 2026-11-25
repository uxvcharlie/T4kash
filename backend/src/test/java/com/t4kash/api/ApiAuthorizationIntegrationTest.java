package com.t4kash.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorre todos los endpoints del API sin sesion: los publicos responden y
 * el resto da 401. La lista de publicos esta escrita a mano a proposito: abrir
 * o cerrar una ruta tiene que ser un cambio visible en este archivo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiAuthorizationIntegrationTest {
    private static final Set<String> EXPECTED_PUBLIC = Set.of(
            "GET /api/health",
            "GET /api/categories",
            "GET /api/tasks",
            "GET /api/tasks/{idTarea}",
            "GET /api/identity/universities",
            "GET /api/identity/universities/{universityId}/careers",
            "GET /api/profiles/{username}",
            "GET /api/identity-verifications/callback",
            "GET /api/payments/pagadito/return",
            "POST /api/auth/register",
            "POST /api/auth/verify-email",
            "POST /api/auth/resend-verification",
            "POST /api/auth/login",
            "POST /api/auth/login/verify",
            "POST /api/auth/login/resend",
            "POST /api/auth/password/forgot",
            "POST /api/auth/password/reset",
            "POST /api/identity-verifications/webhook",
            "POST /api/payments/pagadito/webhook"
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("t4kash")
                    .withUsername("t4kash")
                    .withPassword("t4kash");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("app.rate-limit.auth-requests-per-minute", () -> "1000");
        registry.add("app.rate-limit.email-requests-per-window", () -> "1000");
        registry.add("app.scheduling.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyEndpointRequiresASessionExceptTheExplicitlyPublicOnes() throws Exception {
        Set<String> publicEndpoints = new TreeSet<>();
        List<String> unexpected = new ArrayList<>();
        int checked = 0;

        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                    String endpoint = method.name() + " " + pattern;
                    int status = mockMvc.perform(MockMvcRequestBuilders.request(
                                    HttpMethod.valueOf(method.name()),
                                    pattern.replaceAll("\\{[^}]+}", "1")))
                            .andReturn().getResponse().getStatus();
                    checked++;
                    boolean shouldBePublic = EXPECTED_PUBLIC.contains(endpoint);
                    if (status != 401) {
                        publicEndpoints.add(endpoint);
                    }
                    if (shouldBePublic == (status == 401)) {
                        unexpected.add(endpoint + " -> " + status);
                    }
                }
            }
        }

        assertThat(checked).as("endpoints recorridos").isGreaterThanOrEqualTo(90);
        assertThat(unexpected).as("endpoints con un acceso distinto al esperado").isEmpty();
        assertThat(publicEndpoints).containsExactlyInAnyOrderElementsOf(EXPECTED_PUBLIC);
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        var response = mockMvc.perform(MockMvcRequestBuilders.get("/api/health"))
                .andReturn().getResponse();

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Content-Security-Policy")).contains("frame-ancestors 'none'");
    }

    @Test
    void aBadTokenOnAPrivateRouteGetsAProblemDetail() throws Exception {
        var response = mockMvc.perform(MockMvcRequestBuilders.get("/api/wallet")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-que-no-existe"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"status\":401").contains("La sesion no es valida.");
    }
}
