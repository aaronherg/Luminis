package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.DAO.Administracion.UsuarioAdministracionDataSet;
import com.aarondevs.luminis.Models.Administracion.UsuarioAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionAgregarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionPerfilParameters;
import com.aarondevs.luminis.Services.Seguridad.ContrasenaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdministracionServiceImpl implements UsuarioAdministracionService {
    private final UsuarioAdministracionDataSet datos;
    private final PasswordEncoder passwordEncoder;
    private final ContrasenaService contrasenaService;

    public UsuarioAdministracionServiceImpl(UsuarioAdministracionDataSet datos, PasswordEncoder passwordEncoder, ContrasenaService contrasenaService) {
        this.datos = datos;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaService = contrasenaService;
    }

    @Override
    @Transactional
    public UsuarioAdministracion crear(UsuarioAdministracionAgregarParameters parametro, String actor) {
        validar(parametro);
        if (parametro.getUsuario() == null || !parametro.getUsuario().trim().matches("[A-Za-z0-9._@-]{3,60}")) {
            throw new IllegalArgumentException("El usuario debe tener de 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @");
        }
        if (datos.existsUsuario(parametro.getUsuario())) throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        contrasenaService.validar(parametro.getContrasena());
        return datos.create(parametro, passwordEncoder.encode(parametro.getContrasena()), actor);
    }

    @Override
    @Transactional
    public UsuarioAdministracion actualizar(UsuarioAdministracionActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getUsuarioAdministracionUuid() == null || parametro.getUsuarioAdministracionUuid().isBlank()) {
            throw new IllegalArgumentException("usuarioAdministracionUuid es requerido");
        }
        validar(parametro);
        UsuarioAdministracion actual = datos.findByUuid(parametro.getUsuarioAdministracionUuid());
        if (actual == null) return null;
        return datos.update(parametro, actor);
    }

    @Override
    @Transactional
    public UsuarioAdministracion actualizarPerfil(Long usuarioAdministracionId, UsuarioAdministracionPerfilParameters parametro, String actor) {
        if (parametro == null || parametro.getNombres() == null || parametro.getNombres().isBlank()
                || parametro.getCorreo() == null || !parametro.getCorreo().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("nombres y un correo válido son requeridos");
        }
        if (datos.findById(usuarioAdministracionId) == null) throw new IllegalArgumentException("Usuario no encontrado");
        return datos.actualizarPerfil(usuarioAdministracionId, parametro, actor);
    }

    @Override
    public UsuarioAdministracion id(Long id) {
        return datos.findById(id);
    }

    @Override
    public UsuarioAdministracion uuid(String uuid) {
        return datos.findByUuid(uuid);
    }

    @Override
    public Object all() {
        return datos.findAll();
    }

    @Override
    public void restablecerContrasena(Long id, String contrasena, String actor) {
        contrasenaService.validar(contrasena);
        if (datos.findById(id) == null) throw new IllegalArgumentException("Usuario no encontrado");
        datos.actualizarContrasena(id, passwordEncoder.encode(contrasena), actor);
    }

    @Override
    @Transactional
    public boolean eliminar(Long id, String actor, String usuarioUuidActual) {
        UsuarioAdministracion usuario = datos.findById(id);
        if (usuario == null) return false;
        if (usuario.getUsuarioUuid() != null && usuario.getUsuarioUuid().equals(usuarioUuidActual)) {
            throw new IllegalArgumentException("No puede eliminar su propio usuario");
        }
        return datos.delete(id, actor);
    }

    private void validar(UsuarioAdministracionAgregarParameters parametro) {
        if (parametro == null || parametro.getNombres() == null || parametro.getNombres().isBlank()
                || parametro.getCorreo() == null || !parametro.getCorreo().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("nombres y un correo válido son requeridos");
        }
        if (!datos.rolesValidos(parametro.getRolesIds())) {
            throw new IllegalArgumentException("Uno o más roles no son válidos");
        }
    }
}
