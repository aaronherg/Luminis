package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.DetalleSuscripcion;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RenovacionSuscripcionTarea {
    private static final Logger log = LoggerFactory.getLogger(RenovacionSuscripcionTarea.class);

    private final SuscripcionService suscripciones;

    public RenovacionSuscripcionTarea(SuscripcionService suscripciones) {
        this.suscripciones = suscripciones;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        renovar();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Santo_Domingo")
    public void renovar() {
        LocalDateTime limite = LocalDateTime.now();
        int renovadas = 0;
        for (DetalleSuscripcion detalle : suscripciones.pendientesRenovacion(limite)) {
            try {
                suscripciones.renovarCadena(detalle, limite);
                renovadas++;
            } catch (Exception error) {
                log.error("No se pudo renovar la suscripción {}: {}", detalle.getDetalleSuscripcionId(), error.getMessage());
            }
        }
        if (renovadas > 0) log.info("Suscripciones pospago renovadas: {}", renovadas);
    }
}
