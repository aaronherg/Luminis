package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.SuscripcionSolicitud;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudAgregarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudResponderParameters;

public interface SuscripcionSolicitudService {
    SuscripcionSolicitud crear(SuscripcionSolicitudAgregarParameters parametro, Long clienteId, String actor);

    SuscripcionSolicitud responder(SuscripcionSolicitudResponderParameters parametro, String actor, Long usuarioAdministracionId);

    boolean cancelar(Long suscripcionSolicitudId, String actor);

    SuscripcionSolicitud id(Long suscripcionSolicitudId);

    Object pages(Long clienteId, String estado, Integer page, Integer size);
}
