package com.aarondevs.luminis.Parameters.Suscripciones;

import lombok.Data;

@Data
public class PagoSuscripcionBuscarParameters {
    private Integer page;
    private Integer size;
    private String buscar;
    private String estado;
    private Long clienteId;
}
