package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.DetalleSuscripcion;
import com.aarondevs.luminis.Models.Suscripciones.Suscripcion;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionAsignarParameters;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SuscripcionService {
    String PREPAGO = "PREPAGO";
    String POSTPAGO = "POSTPAGO";
    long PETICIONES_POR_BLOQUE = 1000;

    Suscripcion asignar(SuscripcionAsignarParameters parametro, String actor, Long usuarioAdministracionId);

    int cancelar(Long clienteId, String actor);

    Suscripcion cliente(Long clienteId);

    Object pages(Integer page, Integer size, String buscar);

    DetalleSuscripcion vigente(Long clienteId);

    Object historial(Long clienteId);

    long peticionesUsadas(Long clienteId);

    Map<String, Object> consumo(Long clienteId);

    void renovarCadena(DetalleSuscripcion vencido, LocalDateTime limite);

    List<DetalleSuscripcion> pendientesRenovacion(LocalDateTime limite);
}
