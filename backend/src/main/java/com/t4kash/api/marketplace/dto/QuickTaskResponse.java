package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;

public record QuickTaskResponse(
        TaskResponse tarea,
        double distanciaKm,
        long segundosRestantes
) {
    /**
     * Zona aproximada y distancia redondeada hacia arriba a 0.5 km: con la
     * distancia exacta y varios puntos de origen se podria triangular el
     * domicilio. La ubicacion exacta se ve al tomar la tarea.
     */
    public QuickTaskResponse withApproximateLocation() {
        return new QuickTaskResponse(
                tarea.withApproximateLocation(),
                Math.max(0.5, Math.ceil(distanciaKm / 0.5) * 0.5),
                segundosRestantes
        );
    }

    public QuickTaskResponse withClient(PublicIdentityResponse client) {
        return new QuickTaskResponse(
                tarea.withClient(client),
                distanciaKm,
                segundosRestantes
        );
    }
}
