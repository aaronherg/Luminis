package com.aarondevs.luminis.DAO.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.SuscripcionSolicitud;
import com.aarondevs.luminis.Parameters.Response.PaginationResponse;

public interface SuscripcionSolicitudDataSet {
    PaginationResponse<SuscripcionSolicitud> findAllPages(Long clienteId, String estado, Integer page, Integer size);

    SuscripcionSolicitud findById(Long suscripcionSolicitudId);

    boolean existsPendiente(Long clienteId);

    Long create(Long clienteId, Long planId, String tipoCobro, String comentario, String actor);

    int responder(Long suscripcionSolicitudId, String estado, String respuesta, Long usuarioAdministracionId, String actor);

    int cancelar(Long suscripcionSolicitudId, String actor);
}
