package com.aarondevs.luminis.Parameters.Suscripciones;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PagoSuscripcionAgregarParameters {
    private Long pagoSuscripcionId;
    private BigDecimal monto;
    private String metodo;
    private String referencia;
    private String observacion;
}
