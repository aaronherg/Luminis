package com.aarondevs.luminis.DAO.Clientes;

import com.aarondevs.luminis.Models.Clientes.UsuarioCliente;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePerfilParameters;
import java.util.List;

public interface UsuarioClienteDataSet {
    UsuarioCliente create(UsuarioClienteAgregarParameters parametro, String contrasenaHash, boolean propietario, String actor);

    UsuarioCliente update(UsuarioClienteActualizarParameters parametro, String actor);

    UsuarioCliente actualizarPerfil(Long usuarioClienteId, UsuarioClientePerfilParameters parametro, String actor);

    UsuarioCliente findById(Long id);

    UsuarioCliente findByUuid(String uuid);

    List<UsuarioCliente> findAll(Long clienteId);

    boolean existsUsuario(String usuario);

    boolean rolesValidos(Long clienteId, List<Long> rolesIds);

    void reemplazarRoles(Long usuarioClienteId, List<Long> rolesIds, String actor);

    void actualizarContrasena(Long usuarioClienteId, String contrasenaHash, String actor);

    boolean delete(Long id, String actor);
}
