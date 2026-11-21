package com.aarondevs.luminis.Models.Administracion;

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
public class Plan extends BaseVersionado {
    private Long planId;
    private String planUuid;
    private Long monedaId;
    private String monedaUuid;
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
    private String monedaSimbolo;
    private String monedaCodigo;
}
