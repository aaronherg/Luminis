package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.Models.Clientes.RolCliente;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteAgregarParameters;

public interface RolClienteService {
    Object crear(RolClienteAgregarParameters parametro, String actor);

    Object actualizar(RolClienteActualizarParameters parametro, String actor);

    RolCliente id(Long id);

    RolCliente uuid(String uuid);

    Object all(Long clienteId);

    Object modulos();

    boolean eliminar(Long id, String actor);
}
