package com.t4kash.api;

import com.t4kash.api.identity.service.VerificationEmailService;
import com.t4kash.api.network.entity.Publicacion;
import com.t4kash.api.network.repository.PublicacionRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiPostgresqlIntegrationTest {
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // El filtro por IP tiene su propia prueba; aqui todas las peticiones salen de 127.0.0.1.
        registry.add("app.rate-limit.auth-requests-per-minute", () -> "1000");
        registry.add("app.rate-limit.email-requests-per-window", () -> "1000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PublicacionRepository publicationRepository;

    @MockitoBean
    private VerificationEmailService emailService;

    @Test
    void bootsWithTheProductionSchemaAndExposesHealth() throws Exception {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """,
                Integer.class
        );
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        );
        assertThat(appliedVersions).containsExactly("1", "2");

        assertThat(tableCount).isEqualTo(54);
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void registersVerifiesAndLogsInAgainstTheProductionSchema() throws Exception {
        String correo = "registro.flujo@gmail.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Flujo","correo":"%s","password":"Clave12345"}
                                """.formatted(correo)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> registrationCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendCode(eq(correo), registrationCode.capture(), anyInt());
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}
                                """.formatted(correo, registrationCode.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        UUID publicId = jdbcTemplate.queryForObject(
                "SELECT uuid_publico FROM usuarios WHERE correo = ?",
                UUID.class,
                correo
        );
        assertThat(publicId).isNotNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","password":"Clave12345"}
                                """.formatted(correo)))
                .andExpect(status().isOk());
        ArgumentCaptor<String> loginCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendLoginCode(eq(correo), loginCode.capture(), anyInt());
        mockMvc.perform(post("/api/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}
                                """.formatted(correo, loginCode.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void fcmTokenCanOnlyBeRegisteredForTheSessionAccount() throws Exception {
        Session owner = registerAndLogIn("fcm.duena@gmail.com");
        Session other = registerAndLogIn("fcm.otra@gmail.com");
        String body = "{\"token\":\"dispositivo-de-la-duena\"}";

        mockMvc.perform(put("/api/auth/{id}/fcm-token", owner.userId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-del-atacante\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/auth/{id}/fcm-token", owner.userId())
                        .header(HttpHeaders.AUTHORIZATION, other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-del-atacante\"}"))
                .andExpect(status().isForbidden());
        assertThat(fcmTokenOf(owner.userId())).isNull();

        mockMvc.perform(put("/api/auth/me/fcm-token")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertThat(fcmTokenOf(owner.userId())).isEqualTo("dispositivo-de-la-duena");

        mockMvc.perform(put("/api/auth/me/fcm-token")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"  \"}"))
                .andExpect(status().isBadRequest());

        // El mismo telefono pasa a la otra cuenta: la primera deja de recibir push ahi.
        mockMvc.perform(put("/api/auth/me/fcm-token")
                        .header(HttpHeaders.AUTHORIZATION, other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
        assertThat(fcmTokenOf(owner.userId())).isNull();
        assertThat(fcmTokenOf(other.userId())).isEqualTo("dispositivo-de-la-duena");

        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isNoContent());
        assertThat(fcmTokenOf(other.userId())).isNull();
    }

    @Test
    void emailVerificationStopsAcceptingGuessesAfterFiveWrongCodes() throws Exception {
        String correo = "victima.codigo@gmail.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Vic","apellido":"Tima","correo":"%s","password":"Clave12345"}
                                """.formatted(correo)))
                .andExpect(status().isCreated());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendCode(eq(correo), code.capture(), anyInt());
        String wrongCode = "%06d".formatted((Integer.parseInt(code.getValue()) + 1) % 1_000_000);

        for (int attempt = 0; attempt < 5; attempt++) {
            verifyEmail(correo, wrongCode).andExpect(status().isBadRequest());
        }
        // Ni siquiera el codigo correcto entra mientras dura el bloqueo.
        verifyEmail(correo, code.getValue()).andExpect(status().isTooManyRequests());
    }

    @Test
    void failedLoginsFromOneIpDoNotLockTheOwnerOutFromAnotherIp() throws Exception {
        String correo = "duena.bloqueo@gmail.com";
        registerAndLogIn(correo);

        for (int attempt = 0; attempt < 5; attempt++) {
            login(correo, "ClaveIncorrecta1", "203.0.113.66").andExpect(status().isUnauthorized());
        }
        login(correo, "ClaveIncorrecta1", "203.0.113.66").andExpect(status().isTooManyRequests());
        login(correo, "Clave12345", "198.51.100.20").andExpect(status().isOk());
    }

    @Test
    void exactTaskLocationIsOnlyVisibleToTheOwnerAndTheAssignedStudent() throws Exception {
        Session owner = registerAndLogIn("cliente.ubicacion@gmail.com");
        Session outsider = registerAndLogIn("curioso.ubicacion@gmail.com");
        Session student = registerAndLogIn(
                "estudiante.ubicacion@uni.edu.ni",
                ",\"idUniversidad\":3,\"idCarrera\":33"
        );
        String created = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Llevar un paquete","descripcion":"Entrega en el barrio",
                                 "presupuesto":300,"idCategoria":1,"tipoOportunidad":"RAPIDA",
                                 "modalidad":"PRESENCIAL","direccionReferencia":"Casa azul frente al parque",
                                 "latitud":12.136412,"longitud":-86.251437}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer taskId = JsonPath.read(created, "$.idTarea");

        // Sin sesion y con la sesion de otra persona: zona aproximada, sin direccion.
        for (String bearer : new String[] {null, outsider.bearer()}) {
            var request = get("/api/tasks/{id}", taskId);
            if (bearer != null) {
                request.header(HttpHeaders.AUTHORIZATION, bearer);
            }
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.direccionReferencia").doesNotExist())
                    .andExpect(jsonPath("$.latitud").value(12.135))
                    .andExpect(jsonPath("$.longitud").value(-86.25));
        }
        mockMvc.perform(get("/api/tasks").queryParam("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idTarea == %d)].direccionReferencia".formatted(taskId))
                        .value(Matchers.contains(Matchers.nullValue())))
                .andExpect(jsonPath("$[?(@.idTarea == %d)].latitud".formatted(taskId))
                        .value(Matchers.contains(12.135)));

        mockMvc.perform(get("/api/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
                .andExpect(jsonPath("$.direccionReferencia").value("Casa azul frente al parque"))
                .andExpect(jsonPath("$.latitud").value(12.136412));
        mockMvc.perform(get("/api/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, student.bearer()))
                .andExpect(jsonPath("$.direccionReferencia").doesNotExist());

        jdbcTemplate.update(
                "INSERT INTO trabajos_asignados (id_tarea, id_estudiante) VALUES (?, ?)",
                taskId,
                student.userId()
        );
        mockMvc.perform(get("/api/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, student.bearer()))
                .andExpect(jsonPath("$.direccionReferencia").value("Casa azul frente al parque"))
                .andExpect(jsonPath("$.longitud").value(-86.251437));

        mockMvc.perform(get("/api/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void marketplaceListsRemainBounded() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .queryParam("page", "0")
                        .queryParam("size", "5000"))
                .andExpect(status().isOk());
    }

    @Test
    void networkFeedAppliesVisibilityHidingAndBlocking() {
        jdbcTemplate.update(
                """
                INSERT INTO usuarios (
                    id_usuario, nombre_usuario, nombre, apellido, correo,
                    password_hash, estado_usuario, id_universidad
                ) VALUES
                    (901, 'lector.network', 'Lector', 'Network',
                     'lector.network@example.com', 'hash', 'ACTIVO', 1),
                    (902, 'autor.network', 'Autor', 'Network',
                     'autor.network@example.com', 'hash', 'ACTIVO', 1)
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO conexiones_usuarios (
                    id_usuario_solicitante, id_usuario_receptor, estado_conexion
                ) VALUES (901, 902, 'ACEPTADA')
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO publicaciones (
                    id_publicacion, id_usuario, contenido,
                    tipo_publicacion, visibilidad
                ) VALUES
                    (911, 902, 'Publica', 'TEXTO', 'PUBLICA'),
                    (912, 902, 'Universidad', 'TEXTO', 'UNIVERSIDAD'),
                    (913, 902, 'Conexion', 'TEXTO', 'CONEXIONES')
                """
        );

        List<Publicacion> visible = publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        );
        assertThat(visible).hasSize(3);

        jdbcTemplate.update(
                """
                INSERT INTO publicaciones_ocultas (id_publicacion, id_usuario)
                VALUES (911, 901)
                """
        );
        assertThat(publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        )).hasSize(2);

        jdbcTemplate.update(
                """
                INSERT INTO bloqueos_usuarios (
                    id_usuario_bloquea,
                    id_usuario_bloqueado
                ) VALUES (901, 902)
                """
        );
        assertThat(publicationRepository.findFeed(
                901,
                "PARA_TI",
                PageRequest.of(0, 20)
        )).isEmpty();
    }

    private record Session(Integer userId, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    /** Registra una cuenta de cliente por la API, la verifica y devuelve su sesion. */
    private Session registerAndLogIn(String correo) throws Exception {
        return registerAndLogIn(correo, "");
    }

    /** {@code extraFields} se agrega al JSON del registro (por ejemplo, universidad y carrera). */
    private Session registerAndLogIn(String correo, String extraFields) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Prueba","apellido":"Integracion","correo":"%s","password":"Clave12345"%s}
                                """.formatted(correo, extraFields)))
                .andExpect(status().isCreated());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendCode(eq(correo), code.capture(), anyInt());
        String response = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}
                                """.formatted(correo, code.getValue())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(response, "$.token");
        Integer userId = JsonPath.read(response, "$.usuario.idUsuario");
        return new Session(userId, token);
    }

    private ResultActions verifyEmail(String correo, String codigo) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"correo":"%s","codigo":"%s"}
                        """.formatted(correo, codigo)));
    }

    private ResultActions login(String correo, String password, String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"correo":"%s","password":"%s"}
                        """.formatted(correo, password)));
    }

    private String fcmTokenOf(Integer userId) {
        return jdbcTemplate.queryForObject(
                "SELECT fcm_token FROM usuarios WHERE id_usuario = ?",
                String.class,
                userId
        );
    }
}
