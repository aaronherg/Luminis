package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Models.Administracion.UsuarioAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionAgregarParameters;

public interface UsuarioAdministracionService {
    UsuarioAdministracion crear(UsuarioAdministracionAgregarParameters parametro, String actor);

    UsuarioAdministracion actualizar(UsuarioAdministracionActualizarParameters parametro, String actor);

    UsuarioAdministracion id(Long id);

    UsuarioAdministracion uuid(String uuid);

    Object all();

    void restablecerContrasena(Long id, String contrasena, String actor);

    boolean eliminar(Long id, String actor, String usuarioUuidActual);
}
