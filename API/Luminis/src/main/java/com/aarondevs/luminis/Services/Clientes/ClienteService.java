package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.Models.Clientes.Cliente;
import com.aarondevs.luminis.Parameters.Clientes.ClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.ClienteAgregarParameters;

public interface ClienteService {
    Cliente crear(ClienteAgregarParameters parametro, String actor);

    Cliente actualizar(ClienteActualizarParameters parametro, String actor);

    Cliente id(Long id);

    Cliente uuid(String uuid);

    Object all();

    Object pages(Integer page, Integer size, String buscar, String estado);

    boolean eliminar(Long id, String actor);
}
