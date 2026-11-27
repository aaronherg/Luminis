package com.aarondevs.luminis.DAO.Clientes;

import com.aarondevs.luminis.Models.Clientes.RolCliente;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteAgregarParameters;
import java.util.List;

public interface RolClienteDataSet {
    RolCliente create(RolClienteAgregarParameters parametro, String actor);

    RolCliente update(RolClienteActualizarParameters parametro, String actor);

    RolCliente findById(Long id);

    RolCliente findByUuid(String uuid);

    List<RolCliente> findAll(Long clienteId);

    boolean existsNombre(Long clienteId, String nombre, String excluirUuid);

    boolean enUso(Long id);


    boolean delete(Long id, String actor);

    List<java.util.Map<String, Object>> modulos();
}
