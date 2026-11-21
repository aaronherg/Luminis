package com.aarondevs.luminis.Parameters.Suscripciones;

import lombok.Data;

@Data
public class SuscripcionSolicitudResponderParameters {
    private Long suscripcionSolicitudId;
    private Boolean aprobar;
    private Boolean permitirAdicionales;
    private String respuesta;
}
