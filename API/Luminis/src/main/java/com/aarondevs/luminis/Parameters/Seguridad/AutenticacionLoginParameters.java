package com.aarondevs.luminis.Parameters.Seguridad;

import lombok.Data;

@Data
public class AutenticacionLoginParameters {
    private String codigo;
    private String usuario;
    private String contrasena;
    private String ipPublica;
    private String agente;
}
