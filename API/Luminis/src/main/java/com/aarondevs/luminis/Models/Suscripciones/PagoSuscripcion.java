package com.aarondevs.luminis.Models.Suscripciones;

import com.aarondevs.luminis.Models.BaseVersionado;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagoSuscripcion extends BaseVersionado {
    private Long pagoSuscripcionId;
    private String pagoSuscripcionUuid;
    private Long detalleSuscripcionId;
    private String detalleSuscripcionUuid;
    private Long monedaId;
    private String monedaUuid;
    private BigDecimal montoPlan;
    private Long peticionesAdicionales;
    private BigDecimal montoAdicionales;
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private java.sql.Timestamp fechaVencimiento;
    private String clienteNombre;
    private Long clienteId;
    private String planNombre;
    private java.sql.Timestamp fechaInicio;
    private java.sql.Timestamp fechaFin;
    private String tipoCobro;
    private String monedaSimbolo;
    private BigDecimal montoPendiente;
    private List<PagoSuscripcionDetalle> detalles;
}
