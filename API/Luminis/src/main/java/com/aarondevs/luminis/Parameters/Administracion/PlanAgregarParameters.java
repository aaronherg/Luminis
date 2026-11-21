package com.aarondevs.luminis.Parameters.Administracion;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PlanAgregarParameters {
    private Long monedaId;
    private String nombre;
    private String descripcion;
    private String modalidad;
    private Long peticionesMes;
    private Integer conexionesSimultaneas;
    private Integer tamanoMaximoMensajeKb;
    private Integer retencionSegundos;
    private BigDecimal precio;
    private BigDecimal precioMilAdicional;
    private String caracteristicas;
    private Boolean destacado;
    private Boolean publico;
    private Integer orden;
}
