package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;

@Data
public class UsuarioClienteRestablecerContrasenaParameters {
    private Long usuarioClienteId;
    private String contrasena;
}
