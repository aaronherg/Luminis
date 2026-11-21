package com.aarondevs.luminis.Parameters.Clientes;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ClienteActualizarParameters extends ClienteDatosParameters {
    private String clienteUuid;
    private String estado;
}
