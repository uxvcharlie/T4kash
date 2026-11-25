package com.t4kash.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce el primer arranque con Flyway contra una base como la de
 * produccion: ya tiene el esquema previo (V1) y no tiene historial de Flyway.
 * Flyway debe marcar la linea base sin ejecutarla, aplicar V2 y dejar que
 * Hibernate valide las entidades.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywayBaselineIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("t4kash")
                    .withUsername("t4kash")
                    .withPassword("t4kash")
                    .withInitScript("db/migration/V1__linea_base.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void baselinesTheExistingSchemaAndAppliesOnlyTheNewMigrations() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank"
        );

        assertThat(history).extracting(row -> row.get("version")).containsExactly("1", "2");
        assertThat(history.get(0).get("type")).isEqualTo("BASELINE");
        assertThat(history.get(1).get("type")).isEqualTo("SQL");
        assertThat(history).allMatch(row -> Boolean.TRUE.equals(row.get("success")));

        Integer splitColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'pagos'
                  AND column_name IN ('comision_cliente', 'comision_estudiante')
                """,
                Integer.class
        );
        assertThat(splitColumns).isEqualTo(2);
    }
}
