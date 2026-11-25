package com.t4kash.api.marketplace.service;

import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decide quien ve la ubicacion exacta de una tarea.
 *
 * <p>La direccion y las coordenadas exactas suelen ser el domicilio de quien
 * publica. Solo las ven el cliente que la publico y el estudiante asignado; el resto,
 * incluidas las visitas sin sesion, recibe la zona aproximada.
 */
@Component
public class TaskLocationPolicy {
    private final TrabajoAsignadoRepository jobRepository;

    public TaskLocationPolicy(TrabajoAsignadoRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> apply(List<TaskResponse> tasks, Integer viewerId) {
        if (tasks.isEmpty()) {
            return tasks;
        }
        Set<Integer> assigned = viewerId == null
                ? Set.of()
                : new HashSet<>(jobRepository.findTaskIdsAssignedTo(
                        viewerId,
                        tasks.stream().map(TaskResponse::idTarea).toList()
                ));
        return tasks.stream()
                .map(task -> canSeeExactLocation(task, viewerId, assigned)
                        ? task
                        : task.withApproximateLocation())
                .toList();
    }

    public TaskResponse apply(TaskResponse task, Integer viewerId) {
        return apply(List.of(task), viewerId).get(0);
    }

    private boolean canSeeExactLocation(TaskResponse task, Integer viewerId, Set<Integer> assigned) {
        return viewerId != null
                && (viewerId.equals(task.idCliente()) || assigned.contains(task.idTarea()));
    }
}
