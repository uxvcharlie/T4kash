package com.t4kash.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (MaintenanceJobs). Se pueden apagar con
 * APP_SCHEDULING_ENABLED=false, por ejemplo en pruebas.
 *
 * <p>Corren dentro de la unica instancia del API. Con varias instancias habria
 * que coordinarlas (por ejemplo, con ShedLock) para no repetirlas.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
