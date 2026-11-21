package com.aarondevs.luminis.Parameters.Mensajeria;

import lombok.Data;

@Data
public class MensajeAgregarParameters {
    private String origen;
    private String destino;
    private Object mensaje;
}
