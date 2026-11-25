package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.DAO.Clientes.UsuarioClienteDataSet;
import com.aarondevs.luminis.Models.Clientes.UsuarioCliente;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePerfilParameters;
import com.aarondevs.luminis.Services.Seguridad.ContrasenaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioClienteServiceImpl implements UsuarioClienteService {
    private final UsuarioClienteDataSet datos;
    private final PasswordEncoder passwordEncoder;
    private final ContrasenaService contrasenaService;

    public UsuarioClienteServiceImpl(UsuarioClienteDataSet datos, PasswordEncoder passwordEncoder, ContrasenaService contrasenaService) {
        this.datos = datos;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaService = contrasenaService;
    }

    @Override
    @Transactional
    public UsuarioCliente crear(UsuarioClienteAgregarParameters parametro, boolean propietario, String actor) {
        validar(parametro);
        if (parametro.getUsuario() == null || !parametro.getUsuario().trim().matches("[A-Za-z0-9._@-]{3,60}")) {
            throw new IllegalArgumentException("El usuario debe tener de 3 a 60 caracteres: letras, números, punto, guion, guion bajo o @");
        }
        if (datos.existsUsuario(parametro.getUsuario())) throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        contrasenaService.validar(parametro.getContrasena());
        return datos.create(parametro, passwordEncoder.encode(parametro.getContrasena()), propietario, actor);
    }

    @Override
    @Transactional
    public UsuarioCliente actualizar(UsuarioClienteActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getUsuarioClienteUuid() == null || parametro.getUsuarioClienteUuid().isBlank()) {
            throw new IllegalArgumentException("usuarioClienteUuid es requerido");
        }
        validar(parametro);
        UsuarioCliente actual = datos.findByUuid(parametro.getUsuarioClienteUuid());
        if (actual == null) return null;
        if (Boolean.TRUE.equals(actual.getPropietario()) && parametro.getEstado() != null && !"Activo".equals(parametro.getEstado())) {
            throw new IllegalArgumentException("El usuario propietario no se puede suspender");
        }
        if (Boolean.TRUE.equals(actual.getPropietario())) parametro.setRolesIds(null);
        return datos.update(parametro, actor);
    }

    @Override
    @Transactional
    public UsuarioCliente actualizarPerfil(Long usuarioClienteId, UsuarioClientePerfilParameters parametro, String actor) {
        if (parametro == null || parametro.getNombres() == null || parametro.getNombres().isBlank()
                || parametro.getCorreo() == null || !parametro.getCorreo().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("nombres y un correo válido son requeridos");
        }
        if (datos.findById(usuarioClienteId) == null) throw new IllegalArgumentException("Usuario no encontrado");
        return datos.actualizarPerfil(usuarioClienteId, parametro, actor);
    }

    @Override
    public UsuarioCliente id(Long id) {
        return datos.findById(id);
    }

    @Override
    public UsuarioCliente uuid(String uuid) {
        return datos.findByUuid(uuid);
    }

    @Override
    public Object all(Long clienteId) {
        return datos.findAll(clienteId);
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
        UsuarioCliente usuario = datos.findById(id);
        if (usuario == null) return false;
        if (Boolean.TRUE.equals(usuario.getPropietario())) throw new IllegalArgumentException("El usuario propietario no se puede eliminar");
        if (usuario.getUsuarioUuid() != null && usuario.getUsuarioUuid().equals(usuarioUuidActual)) {
            throw new IllegalArgumentException("No puede eliminar su propio usuario");
        }
        return datos.delete(id, actor);
    }

    private void validar(UsuarioClienteAgregarParameters parametro) {
        if (parametro == null || parametro.getNombres() == null || parametro.getNombres().isBlank()
                || parametro.getCorreo() == null || !parametro.getCorreo().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("nombres y un correo válido son requeridos");
        }
        if (!datos.rolesValidos(parametro.getClienteId(), parametro.getRolesIds())) {
            throw new IllegalArgumentException("Uno o más roles no son válidos");
        }
    }
}
