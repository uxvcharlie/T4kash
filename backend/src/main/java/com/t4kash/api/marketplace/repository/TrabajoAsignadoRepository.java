package com.t4kash.api.marketplace.repository;

import com.t4kash.api.marketplace.entity.TrabajoAsignado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrabajoAsignadoRepository extends JpaRepository<TrabajoAsignado, Integer> {
    Optional<TrabajoAsignado> findByIdTarea(Integer idTarea);

    @Query("""
            SELECT trabajo.idTarea
            FROM TrabajoAsignado trabajo
            WHERE trabajo.idEstudiante = :idEstudiante
              AND trabajo.idTarea IN :idsTarea
            """)
    List<Integer> findTaskIdsAssignedTo(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("idsTarea") Collection<Integer> idsTarea
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT trabajo FROM TrabajoAsignado trabajo WHERE trabajo.idTrabajo = :idTrabajo")
    Optional<TrabajoAsignado> findByIdForUpdate(@Param("idTrabajo") Integer idTrabajo);

    long countByIdEstudianteAndEstadoTrabajo(
            Integer idEstudiante,
            String estadoTrabajo
    );

    @Query(
            value = """
            SELECT trabajo.*
            FROM trabajos_asignados trabajo
            INNER JOIN tareas tarea ON tarea.id_tarea = trabajo.id_tarea
            WHERE (
                    trabajo.id_estudiante = :idUsuario
                    OR tarea.id_cliente = :idUsuario
            )
            ORDER BY trabajo.fecha_inicio DESC
            """,
            nativeQuery = true
    )
    List<TrabajoAsignado> findVisibleToUser(@Param("idUsuario") Integer idUsuario);
}
