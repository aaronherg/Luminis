package com.aarondevs.luminis.DAO.Seguridad;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PermisoDAO implements PermisoDataSet {
    private final JdbcTemplate jdbc;

    public PermisoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Long asegurarRolAdministracionProtegido(String nombre) {
        Long id = jdbc.query("SELECT rolAdministracionId FROM RolesAdministracion WHERE nombre=? AND estado<>'Eliminado'",
                (rs, i) -> rs.getLong(1), nombre).stream().findFirst().orElse(null);
        if (id != null) {
            jdbc.update("UPDATE RolesAdministracion SET protegido=TRUE WHERE rolAdministracionId=? AND protegido=FALSE", id);
            return id;
        }
        jdbc.update("INSERT INTO RolesAdministracion (rolAdministracionUuid, nombre, descripcion, color, protegido, usuarioCreacionUuid) "
                + "VALUES (?, ?, 'Acceso completo al área administrativa.', '#e3121b', TRUE, 'SISTEMA')", UUID.randomUUID().toString(), nombre);
        return jdbc.queryForObject("SELECT rolAdministracionId FROM RolesAdministracion WHERE nombre=? AND estado<>'Eliminado'", Long.class, nombre);
    }

    @Override
    public Long findUsuarioIdByUsuario(String usuario) {
        return jdbc.query("SELECT usuarioId FROM Usuarios WHERE usuario=? AND estado<>'Eliminado'",
                (rs, i) -> rs.getLong(1), usuario).stream().findFirst().orElse(null);
    }

    @Override
    public Long crearUsuario(String usuario, String contrasenaHash, String tipo, boolean debeCambiarContrasena, String actor) {
        jdbc.update("INSERT INTO Usuarios (usuarioUuid, usuario, contrasena, tipo, debeCambiarContrasena, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID().toString(), usuario, contrasenaHash, tipo, debeCambiarContrasena, actor);
        return findUsuarioIdByUsuario(usuario);
    }

    @Override
    public Long crearUsuarioAdministracion(Long usuarioId, String nombres, String apellidos, String correo, String actor) {
        String uuid = UUID.randomUUID().toString();
        String usuarioUuid = jdbc.queryForObject("SELECT usuarioUuid FROM Usuarios WHERE usuarioId=?", String.class, usuarioId);
        jdbc.update("INSERT INTO UsuariosAdministracion (usuarioAdministracionUuid, usuarioId, usuarioUuid, nombres, apellidos, correo, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", uuid, usuarioId, usuarioUuid, nombres, apellidos, correo, actor);
        return jdbc.queryForObject("SELECT usuarioAdministracionId FROM UsuariosAdministracion WHERE usuarioAdministracionUuid=?", Long.class, uuid);
    }

    @Override
    public void asignarRolAdministracion(Long usuarioAdministracionId, Long rolAdministracionId, String actor) {
        jdbc.update("INSERT IGNORE INTO UsuariosAdministracionRoles (usuarioAdministracionRolUuid, usuarioAdministracionId, usuarioAdministracionUuid, "
                + "rolAdministracionId, rolAdministracionUuid, usuarioCreacionUuid) "
                + "SELECT UUID(), u.usuarioAdministracionId, u.usuarioAdministracionUuid, r.rolAdministracionId, r.rolAdministracionUuid, ? "
                + "FROM UsuariosAdministracion u CROSS JOIN RolesAdministracion r WHERE u.usuarioAdministracionId=? AND r.rolAdministracionId=?",
                actor, usuarioAdministracionId, rolAdministracionId);
    }
}
