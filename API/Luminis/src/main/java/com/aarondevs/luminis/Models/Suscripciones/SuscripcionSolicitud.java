package com.aarondevs.luminis.Models.Suscripciones;

import com.aarondevs.luminis.Models.BaseVersionado;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SuscripcionSolicitud extends BaseVersionado {
    private Long suscripcionSolicitudId;
    private String suscripcionSolicitudUuid;
    private Long clienteId;
    private String clienteUuid;
    private Long planId;
    private String planUuid;
    private String tipoCobro;
    private String comentario;
    private java.sql.Timestamp fechaRespuesta;
    private Long usuarioAdministracionId;
    private String usuarioAdministracionUuid;
    private String respuesta;
    private String clienteNombre;
    private String planNombre;
}
