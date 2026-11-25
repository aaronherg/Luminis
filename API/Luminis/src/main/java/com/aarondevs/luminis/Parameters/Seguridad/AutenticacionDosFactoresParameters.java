package com.aarondevs.luminis.Parameters.Seguridad;

import lombok.Data;

@Data
public class AutenticacionDosFactoresParameters {
    private String codigo;
    private String contrasena;
}
