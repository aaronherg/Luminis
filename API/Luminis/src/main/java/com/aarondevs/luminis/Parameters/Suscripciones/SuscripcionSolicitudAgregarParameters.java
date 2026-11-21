package com.aarondevs.luminis.Parameters.Suscripciones;

import lombok.Data;

@Data
public class SuscripcionSolicitudAgregarParameters {
    private Long planId;
    private String tipoCobro;
    private String comentario;
}
