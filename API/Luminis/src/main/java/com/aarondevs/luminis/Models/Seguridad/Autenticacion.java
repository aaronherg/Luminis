package com.aarondevs.luminis.Models.Seguridad;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Autenticacion {
    private Long autenticacionId;
    private String autenticacionUuid;
    private Long usuarioId;
    private String usuarioUuid;
    private String tipoUsuario;
    private String tokenJti;
    private String ipPublica;
    private String agente;
    private java.sql.Timestamp fechaAutenticacion;
    private java.sql.Timestamp fechaUltimaActividad;
    private java.sql.Timestamp fechaExpiracion;
    private java.sql.Timestamp fechaCierre;
    private String motivoCierre;
    private String estado;
}
