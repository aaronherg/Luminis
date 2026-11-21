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
    public Long sincronizarModulo(String area, String clave, String nombre, String descripcion, String icono, Integer orden,
                                  String categoria, String categoriaNombre, String categoriaIcono, Integer categoriaOrden) {
        String tabla = "Modulos" + area;
        String singular = singular(area);
        Long id = jdbc.query("SELECT modulo" + singular + "Id FROM " + tabla + " WHERE clave=?",
                (rs, i) -> rs.getLong(1), clave).stream().findFirst().orElse(null);
        if (id == null) {
            jdbc.update("INSERT INTO " + tabla + " (modulo" + singular + "Uuid, clave, nombre, descripcion, icono, orden, categoria, categoriaNombre, "
                    + "categoriaIcono, categoriaOrden, usuarioCreacionUuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SISTEMA')",
                    UUID.randomUUID().toString(), clave, nombre, descripcion, icono, orden, categoria, categoriaNombre, categoriaIcono, categoriaOrden);
            return jdbc.queryForObject("SELECT modulo" + singular + "Id FROM " + tabla + " WHERE clave=?", Long.class, clave);
        }
        jdbc.update("UPDATE " + tabla + " SET nombre=?, descripcion=?, icono=?, orden=?, categoria=?, categoriaNombre=?, categoriaIcono=?, "
                + "categoriaOrden=?, estado='Activo' WHERE modulo" + singular + "Id=?",
                nombre, descripcion, icono, orden, categoria, categoriaNombre, categoriaIcono, categoriaOrden, id);
        return id;
    }

    @Override
    public void sincronizarAccion(String area, Long moduloId, String clave, String nombre, Integer orden) {
        String tabla = "Modulos" + area + "Acciones";
        String singular = singular(area);
        String moduloUuid = jdbc.queryForObject("SELECT modulo" + singular + "Uuid FROM Modulos" + area + " WHERE modulo" + singular + "Id=?",
                String.class, moduloId);
        int actualizadas = jdbc.update("UPDATE " + tabla + " SET nombre=?, orden=?, estado='Activo' WHERE modulo" + singular + "Id=? AND clave=?",
                nombre, orden, moduloId, clave);
        if (actualizadas == 0) {
            jdbc.update("INSERT INTO " + tabla + " (modulo" + singular + "AccionUuid, modulo" + singular + "Id, modulo" + singular + "Uuid, clave, nombre, orden, usuarioCreacionUuid) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 'SISTEMA')", UUID.randomUUID().toString(), moduloId, moduloUuid, clave, nombre, orden);
        }
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
    public void otorgarAccionesARolesProtegidos() {
        jdbc.update("INSERT IGNORE INTO RolesAdministracionAcciones (rolAdministracionAccionUuid, rolAdministracionId, rolAdministracionUuid, "
                + "moduloAdministracionAccionId, moduloAdministracionAccionUuid, usuarioCreacionUuid) "
                + "SELECT UUID(), r.rolAdministracionId, r.rolAdministracionUuid, a.moduloAdministracionAccionId, a.moduloAdministracionAccionUuid, 'SISTEMA' "
                + "FROM RolesAdministracion r CROSS JOIN ModulosAdministracionAcciones a "
                + "WHERE r.protegido=TRUE AND r.estado<>'Eliminado' AND a.estado='Activo'");
        jdbc.update("INSERT IGNORE INTO RolesClientesAcciones (rolClienteAccionUuid, rolClienteId, rolClienteUuid, "
                + "moduloClienteAccionId, moduloClienteAccionUuid, usuarioCreacionUuid) "
                + "SELECT UUID(), r.rolClienteId, r.rolClienteUuid, a.moduloClienteAccionId, a.moduloClienteAccionUuid, 'SISTEMA' "
                + "FROM RolesClientes r CROSS JOIN ModulosClientesAcciones a "
                + "WHERE r.protegido=TRUE AND r.estado<>'Eliminado' AND a.estado='Activo'");
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

    private String singular(String area) {
        return "Administracion".equals(area) ? "Administracion" : "Cliente";
    }
}
