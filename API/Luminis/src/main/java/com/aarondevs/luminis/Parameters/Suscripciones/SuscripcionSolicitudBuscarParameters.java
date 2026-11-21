package com.aarondevs.luminis.Parameters.Suscripciones;

import lombok.Data;

@Data
public class SuscripcionSolicitudBuscarParameters {
    private Integer page;
    private Integer size;
    private String estado;
    private Long clienteId;
}
