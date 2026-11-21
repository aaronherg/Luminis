package com.aarondevs.luminis.Parameters.Seguridad;

import lombok.Data;

@Data
public class AutenticacionCambiarContrasenaParameters {
    private String contrasenaActual;
    private String contrasenaNueva;
}
