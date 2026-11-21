package com.aarondevs.luminis.Parameters.Suscripciones;

import lombok.Data;

@Data
public class SuscripcionBuscarParameters {
    private Integer page;
    private Integer size;
    private String buscar;
}
