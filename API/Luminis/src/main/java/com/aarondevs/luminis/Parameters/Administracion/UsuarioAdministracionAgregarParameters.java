package com.aarondevs.luminis.Parameters.Administracion;

import java.util.List;
import lombok.Data;

@Data
public class UsuarioAdministracionAgregarParameters {
    private String usuario;
    private String contrasena;
    private String nombres;
    private String apellidos;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private List<Long> rolesIds;
}
