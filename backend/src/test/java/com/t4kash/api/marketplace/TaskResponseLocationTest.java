package com.t4kash.api.marketplace;

import com.t4kash.api.marketplace.dto.QuickTaskResponse;
import com.t4kash.api.marketplace.dto.TaskResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaskResponseLocationTest {
    @Test
    void approximateLocationDropsTheAddressAndSnapsToTheGrid() {
        TaskResponse approximate = task("12.136412", "-86.251437").withApproximateLocation();

        assertThat(approximate.direccionReferencia()).isNull();
        assertThat(approximate.latitud()).isEqualByComparingTo("12.135");
        assertThat(approximate.longitud()).isEqualByComparingTo("-86.250");
        assertThat(approximate.titulo()).isEqualTo("Tarea");
    }

    @Test
    void nearbyPointsInTheSameCellLookTheSame() {
        TaskResponse first = task("12.1351", "-86.2501").withApproximateLocation();
        TaskResponse second = task("12.1374", "-86.2524").withApproximateLocation();

        assertThat(first.latitud()).isEqualByComparingTo(second.latitud());
        assertThat(first.longitud()).isEqualByComparingTo(second.longitud());
    }

    @Test
    void keepsMissingCoordinatesMissing() {
        TaskResponse approximate = task(null, null).withApproximateLocation();

        assertThat(approximate.latitud()).isNull();
        assertThat(approximate.longitud()).isNull();
    }

    @Test
    void quickTaskDistanceIsRoundedUpToHalfAKilometer() {
        assertThat(new QuickTaskResponse(task("12.1", "-86.2"), 0.12, 60)
                .withApproximateLocation().distanciaKm()).isEqualTo(0.5);
        assertThat(new QuickTaskResponse(task("12.1", "-86.2"), 1.01, 60)
                .withApproximateLocation().distanciaKm()).isEqualTo(1.5);
    }

    private TaskResponse task(String latitude, String longitude) {
        return new TaskResponse(
                1, "Tarea", "Descripcion", new BigDecimal("300"), null, null, null,
                "PUBLICADA", 1, 7, "RAPIDA", "PRESENCIAL", "PUBLICA",
                "Casa azul frente al parque",
                latitude == null ? null : new BigDecimal(latitude),
                longitude == null ? null : new BigDecimal(longitude),
                null
        );
    }
}
