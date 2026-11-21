package com.aarondevs.luminis.Parameters.Clientes;

import java.util.List;
import lombok.Data;

@Data
public class UsuarioClienteAgregarParameters {
    private Long clienteId;
    private String usuario;
    private String contrasena;
    private String nombres;
    private String apellidos;
    private String correo;
    private String prefijoTelefono;
    private String telefono;
    private List<Long> rolesIds;
}
