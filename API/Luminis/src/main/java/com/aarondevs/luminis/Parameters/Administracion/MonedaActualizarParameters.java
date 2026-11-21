package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MonedaActualizarParameters extends MonedaAgregarParameters {
    private String monedaUuid;
    private String estado;
}
