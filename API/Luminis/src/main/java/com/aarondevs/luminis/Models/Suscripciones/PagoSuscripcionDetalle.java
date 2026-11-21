package com.aarondevs.luminis.Models.Suscripciones;

import com.aarondevs.luminis.Models.BaseVersionado;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagoSuscripcionDetalle extends BaseVersionado {
    private Long pagoDetalleId;
    private String pagoDetalleUuid;
    private Long pagoSuscripcionId;
    private String pagoSuscripcionUuid;
    private BigDecimal monto;
    private String metodo;
    private String referencia;
    private String observacion;
    private java.sql.Timestamp fechaPago;
    private Long usuarioAdministracionId;
    private String usuarioAdministracionUuid;
}
