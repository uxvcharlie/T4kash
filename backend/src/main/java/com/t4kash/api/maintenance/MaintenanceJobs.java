package com.t4kash.api.maintenance;

import com.t4kash.api.finance.repository.PagoRepository;
import com.t4kash.api.finance.service.PaymentService;
import com.t4kash.api.marketplace.repository.TareaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tareas periodicas del API.
 *
 * <p>Render gratis duerme el servicio cuando no hay trafico: mientras duerme
 * no corre nada, y al despertar cada tarea corre a los pocos segundos. Las
 * acciones sensibles (postularse, tomar una tarea) siguen revisando el
 * vencimiento en el momento, asi que no dependen de este horario.
 */
@Component
public class MaintenanceJobs {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceJobs.class);

    private final TareaRepository tareaRepository;
    private final PagoRepository pagoRepository;
    private final int heldFundsAlertDays;

    public MaintenanceJobs(
            TareaRepository tareaRepository,
            PagoRepository pagoRepository,
            @Value("${app.payments.held-funds-alert-days:14}") int heldFundsAlertDays
    ) {
        this.tareaRepository = tareaRepository;
        this.pagoRepository = pagoRepository;
        this.heldFundsAlertDays = heldFundsAlertDays;
    }

    /** Cierra las oportunidades publicadas cuyo plazo de postulacion ya paso. */
    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "${app.scheduling.close-tasks-every:PT5M}")
    @Transactional
    public int closeExpiredTasks() {
        int closed = tareaRepository.closeExpiredPublishedTasks(LocalDateTime.now());
        if (closed > 0) {
            LOGGER.info("Tareas vencidas cerradas: {}", closed);
        }
        return closed;
    }

    /**
     * Avisa en los logs si hay fondos retenidos por mas de N dias: el cliente
     * no aprobo ni abrio una disputa y el estudiante sigue sin cobrar.
     */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT24H")
    @Transactional(readOnly = true)
    public long warnAboutLongHeldFunds() {
        long stuck = pagoRepository.countByEstadoPagoAndFechaConfirmacionBefore(
                PaymentService.FUNDS_HELD,
                LocalDateTime.now().minusDays(heldFundsAlertDays)
        );
        if (stuck > 0) {
            LOGGER.warn(
                    "Hay {} pago(s) con fondos retenidos hace mas de {} dias: revisar en el panel de administracion.",
                    stuck,
                    heldFundsAlertDays
            );
        }
        return stuck;
    }
}
