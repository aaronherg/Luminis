package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UsuarioClienteActualizarParameters extends UsuarioClienteAgregarParameters {
    private String usuarioClienteUuid;
    private String estado;
}
