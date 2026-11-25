package com.t4kash.api.marketplace.dto;

import com.t4kash.api.identity.dto.PublicIdentityResponse;
import com.t4kash.api.marketplace.entity.Tarea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record TaskResponse(
        Integer idTarea,
        String titulo,
        String descripcion,
        BigDecimal presupuesto,
        LocalDateTime fechaPublicacion,
        LocalDateTime fechaLimitePostulacion,
        LocalDateTime fechaLimite,
        String estadoTarea,
        Integer idCategoria,
        Integer idCliente,
        String tipoOportunidad,
        String modalidad,
        String visibilidad,
        String direccionReferencia,
        BigDecimal latitud,
        BigDecimal longitud,
        PublicIdentityResponse cliente
) {
    private static final BigDecimal LOCATION_GRID = new BigDecimal("0.005");

    public static TaskResponse fromEntity(Tarea tarea) {
        return new TaskResponse(
                tarea.getIdTarea(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.getPresupuesto(),
                tarea.getFechaPublicacion(),
                tarea.getFechaLimitePostulacion(),
                tarea.getFechaLimite(),
                tarea.getEstadoTarea(),
                tarea.getIdCategoria(),
                tarea.getIdCliente(),
                tarea.getTipoOportunidad(),
                tarea.getModalidad(),
                tarea.getVisibilidad(),
                tarea.getDireccionReferencia(),
                tarea.getLatitud(),
                tarea.getLongitud(),
                null
        );
    }

    /**
     * Version para quien no es parte de la tarea: sin direccion y con
     * coordenadas redondeadas a una cuadricula de ~550 m. Alcanza para ver la
     * zona en el mapa sin revelar el domicilio de nadie.
     */
    public TaskResponse withApproximateLocation() {
        return new TaskResponse(
                idTarea, titulo, descripcion, presupuesto, fechaPublicacion,
                fechaLimitePostulacion, fechaLimite, estadoTarea, idCategoria,
                idCliente, tipoOportunidad, modalidad, visibilidad,
                null, approximate(latitud), approximate(longitud), cliente
        );
    }

    static BigDecimal approximate(BigDecimal coordinate) {
        if (coordinate == null) {
            return null;
        }
        return coordinate
                .divide(LOCATION_GRID, 0, RoundingMode.HALF_UP)
                .multiply(LOCATION_GRID)
                .setScale(3, RoundingMode.HALF_UP);
    }

    public TaskResponse withClient(PublicIdentityResponse publicClient) {
        return new TaskResponse(
                idTarea, titulo, descripcion, presupuesto, fechaPublicacion,
                fechaLimitePostulacion, fechaLimite, estadoTarea, idCategoria,
                idCliente, tipoOportunidad, modalidad, visibilidad,
                direccionReferencia, latitud, longitud, publicClient
        );
    }
}
