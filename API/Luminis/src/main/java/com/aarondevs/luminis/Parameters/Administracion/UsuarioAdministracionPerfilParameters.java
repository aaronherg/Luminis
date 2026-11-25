package com.aarondevs.luminis.Parameters.Administracion;

import lombok.Data;

@Data
public class UsuarioAdministracionPerfilParameters {
    private String nombres;
    private String apellidos;
    private String documento;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private String whatsapp;
    private String direccion;
}
