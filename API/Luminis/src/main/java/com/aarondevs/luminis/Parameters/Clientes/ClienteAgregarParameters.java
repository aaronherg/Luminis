package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ClienteAgregarParameters extends ClienteDatosParameters {
    private String usuario;
    private String contrasena;
    private String nombres;
    private String apellidos;
}
