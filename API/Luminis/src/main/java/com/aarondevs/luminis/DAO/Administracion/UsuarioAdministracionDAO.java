package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.UsuarioAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.UsuarioAdministracionAgregarParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioAdministracionDAO implements UsuarioAdministracionDataSet {
    private static final String SELECT = "SELECT x.*, u.usuario FROM UsuariosAdministracion x JOIN Usuarios u ON u.usuarioId=x.usuarioId ";

    private final JdbcTemplate jdbc;

    public UsuarioAdministracionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UsuarioAdministracion create(UsuarioAdministracionAgregarParameters parametro, String contrasenaHash, String actor) {
        String usuarioUuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO Usuarios (usuarioUuid, usuario, contrasena, tipo, usuarioCreacionUuid) VALUES (?, ?, ?, 'Administracion', ?)",
                usuarioUuid, parametro.getUsuario().trim(), contrasenaHash, actor);
        Long usuarioId = jdbc.queryForObject("SELECT usuarioId FROM Usuarios WHERE usuarioUuid=?", Long.class, usuarioUuid);
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO UsuariosAdministracion (usuarioAdministracionUuid, usuarioId, usuarioUuid, nombres, apellidos, correo, prefijoTelefono, telefono, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", uuid, usuarioId, usuarioUuid, 
                parametro.getNombres().trim(), parametro.getApellidos(), parametro.getCorreo().trim(), parametro.getPrefijoTelefono(),
                parametro.getTelefono(), actor);
        UsuarioAdministracion creado = findByUuid(uuid);
        reemplazarRoles(creado.getUsuarioAdministracionId(), parametro.getRolesIds(), actor);
        return findByUuid(uuid);
    }

    @Override
    public UsuarioAdministracion update(UsuarioAdministracionActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE UsuariosAdministracion SET nombres=?, apellidos=?, correo=?, prefijoTelefono=?, telefono=?, "
                        + "estado=COALESCE(NULLIF(?,''),estado), fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                        + "usuarioModificacionUuid=? WHERE usuarioAdministracionUuid=? AND estado<>'Eliminado'",
                parametro.getNombres().trim(), parametro.getApellidos(), parametro.getCorreo().trim(), parametro.getPrefijoTelefono(),
                parametro.getTelefono(), parametro.getEstado(), actor, parametro.getUsuarioAdministracionUuid());
        if (actualizados == 0) return null;
        UsuarioAdministracion actualizado = findByUuid(parametro.getUsuarioAdministracionUuid());
        jdbc.update("UPDATE Usuarios SET estado=?, fechaModificacion=NOW(3), usuarioModificacionUuid=? WHERE usuarioId=?",
                actualizado.getEstado(), actor, actualizado.getUsuarioId());
        if (parametro.getRolesIds() != null) reemplazarRoles(actualizado.getUsuarioAdministracionId(), parametro.getRolesIds(), actor);
        return findByUuid(parametro.getUsuarioAdministracionUuid());
    }

    @Override
    public UsuarioAdministracion findById(Long id) {
        return completar(jdbc.query(SELECT + "WHERE x.usuarioAdministracionId=? AND x.estado<>'Eliminado'", new BeanPropertyRowMapper<>(UsuarioAdministracion.class), id)
                .stream().findFirst().orElse(null));
    }

    @Override
    public UsuarioAdministracion findByUuid(String uuid) {
        return completar(jdbc.query(SELECT + "WHERE x.usuarioAdministracionUuid=?", new BeanPropertyRowMapper<>(UsuarioAdministracion.class), uuid)
                .stream().findFirst().orElse(null));
    }

    @Override
    public List<UsuarioAdministracion> findAll() {
        List<UsuarioAdministracion> usuarios = jdbc.query(SELECT + "WHERE x.estado<>'Eliminado' ORDER BY x.nombres, x.apellidos",
                new BeanPropertyRowMapper<>(UsuarioAdministracion.class));
        usuarios.forEach(this::completar);
        return usuarios;
    }

    @Override
    public boolean existsUsuario(String usuario) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Usuarios WHERE usuario=? AND estado<>'Eliminado'", Integer.class, usuario.trim());
        return total != null && total > 0;
    }

    @Override
    public boolean rolesValidos(List<Long> rolesIds) {
        if (rolesIds == null || rolesIds.isEmpty()) return true;
        for (Long rolId : rolesIds) {
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM RolesAdministracion WHERE rolAdministracionId=? AND estado='Activo'",
                    Integer.class, rolId);
            if (total == null || total == 0) return false;
        }
        return true;
    }

    @Override
    public void reemplazarRoles(Long usuarioAdministracionId, List<Long> rolesIds, String actor) {
        jdbc.update("DELETE FROM UsuariosAdministracionRoles WHERE usuarioAdministracionId=?", usuarioAdministracionId);
        if (rolesIds == null) return;
        for (Long rolId : rolesIds.stream().distinct().toList()) {
            jdbc.update("INSERT IGNORE INTO UsuariosAdministracionRoles (usuarioAdministracionRolUuid, usuarioAdministracionId, usuarioAdministracionUuid, rolAdministracionId, rolAdministracionUuid, usuarioCreacionUuid) "
                    + "SELECT UUID(), x.usuarioAdministracionId, x.usuarioAdministracionUuid, r.rolAdministracionId, r.rolAdministracionUuid, ? FROM UsuariosAdministracion x CROSS JOIN RolesAdministracion r "
                    + "WHERE x.usuarioAdministracionId=? AND r.rolAdministracionId=?", actor, usuarioAdministracionId, rolId);
        }
    }

    @Override
    public void actualizarContrasena(Long usuarioAdministracionId, String contrasenaHash, String actor) {
        jdbc.update("UPDATE Usuarios u JOIN UsuariosAdministracion x ON x.usuarioId=u.usuarioId SET u.contrasena=?, u.debeCambiarContrasena=TRUE, "
                + "u.fechaModificacion=NOW(3), u.fechaVersion=NOW(3), u.version=u.version+1, u.usuarioModificacionUuid=? WHERE x.usuarioAdministracionId=?",
                contrasenaHash, actor, usuarioAdministracionId);
    }

    @Override
    public boolean delete(Long id, String actor) {
        UsuarioAdministracion existente = findById(id);
        if (existente == null) return false;
        jdbc.update("UPDATE Usuarios SET estado='Eliminado', fechaModificacion=NOW(3), usuarioModificacionUuid=? WHERE usuarioId=?",
                actor, existente.getUsuarioId());
        return jdbc.update("UPDATE UsuariosAdministracion SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE usuarioAdministracionId=? AND estado<>'Eliminado'", actor, id) > 0;
    }

    private UsuarioAdministracion completar(UsuarioAdministracion usuario) {
        if (usuario != null) {
            usuario.setRolesIds(jdbc.queryForList("SELECT rolAdministracionId FROM UsuariosAdministracionRoles WHERE usuarioAdministracionId=? AND estado='Activo'",
                    Long.class, usuario.getUsuarioAdministracionId()));
        }
        return usuario;
    }
}
