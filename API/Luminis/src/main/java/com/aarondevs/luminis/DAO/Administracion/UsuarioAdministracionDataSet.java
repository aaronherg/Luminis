package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.UsuarioAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionAgregarParameters;
import java.util.List;

public interface UsuarioAdministracionDataSet {
    UsuarioAdministracion create(UsuarioAdministracionAgregarParameters parametro, String contrasenaHash, String actor);

    UsuarioAdministracion update(UsuarioAdministracionActualizarParameters parametro, String actor);

    UsuarioAdministracion findById(Long id);

    UsuarioAdministracion findByUuid(String uuid);

    List<UsuarioAdministracion> findAll();

    boolean existsUsuario(String usuario);

    boolean rolesValidos(List<Long> rolesIds);

    void reemplazarRoles(Long usuarioAdministracionId, List<Long> rolesIds, String actor);

    void actualizarContrasena(Long usuarioAdministracionId, String contrasenaHash, String actor);

    boolean delete(Long id, String actor);
}
