package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;

@Data
public class ServicioSmtpParameters {
    private Boolean habilitado;
    private String host;
    private String puerto;
    private String seguridad;
    private String usuario;
    private String contrasena;
    private String remitente;
    private String nombreRemitente;
}
