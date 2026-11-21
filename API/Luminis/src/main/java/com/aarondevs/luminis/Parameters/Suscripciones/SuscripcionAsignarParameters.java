package com.aarondevs.luminis.Parameters.Suscripciones;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SuscripcionAsignarParameters {
    private Long clienteId;
    private Long planId;
    private String tipoCobro;
    private Boolean permitirAdicionales;
    private BigDecimal montoPagado;
    private String metodoPago;
    private String referenciaPago;
}
