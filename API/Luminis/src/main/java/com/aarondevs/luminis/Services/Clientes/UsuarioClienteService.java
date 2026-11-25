package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.Models.Clientes.UsuarioCliente;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePerfilParameters;

public interface UsuarioClienteService {
    UsuarioCliente crear(UsuarioClienteAgregarParameters parametro, boolean propietario, String actor);

    UsuarioCliente actualizar(UsuarioClienteActualizarParameters parametro, String actor);

    UsuarioCliente actualizarPerfil(Long usuarioClienteId, UsuarioClientePerfilParameters parametro, String actor);

    UsuarioCliente id(Long id);

    UsuarioCliente uuid(String uuid);

    Object all(Long clienteId);

    void restablecerContrasena(Long id, String contrasena, String actor);

    boolean eliminar(Long id, String actor, String usuarioUuidActual);
}
