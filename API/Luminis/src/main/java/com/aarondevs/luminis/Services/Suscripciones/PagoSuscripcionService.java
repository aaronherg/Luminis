package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.PagoSuscripcion;
import java.math.BigDecimal;

public interface PagoSuscripcionService {
    PagoSuscripcion registrar(Long pagoSuscripcionId, BigDecimal monto, String metodo, String referencia, String observacion,
                              String actor, Long usuarioAdministracionId);

    Object pages(Long clienteId, String estado, String buscar, Integer page, Integer size);

    PagoSuscripcion id(Long pagoSuscripcionId);

    Long clienteDeSuscripcion(Long suscripcionId);
}
