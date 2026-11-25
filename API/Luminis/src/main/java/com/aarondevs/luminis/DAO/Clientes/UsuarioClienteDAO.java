package com.aarondevs.luminis.DAO.Clientes;

import com.aarondevs.luminis.Models.Clientes.UsuarioCliente;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.UsuarioClientePerfilParameters;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioClienteDAO implements UsuarioClienteDataSet {
    private static final String SELECT = "SELECT x.*, u.usuario FROM UsuariosClientes x JOIN Usuarios u ON u.usuarioId=x.usuarioId ";

    private final JdbcTemplate jdbc;

    public UsuarioClienteDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UsuarioCliente create(UsuarioClienteAgregarParameters parametro, String contrasenaHash, boolean propietario, String actor) {
        String usuarioUuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO Usuarios (usuarioUuid, usuario, contrasena, tipo, usuarioCreacionUuid) VALUES (?, ?, ?, 'Cliente', ?)",
                usuarioUuid, parametro.getUsuario().trim(), contrasenaHash, actor);
        Long usuarioId = jdbc.queryForObject("SELECT usuarioId FROM Usuarios WHERE usuarioUuid=?", Long.class, usuarioUuid);
        String uuid = UUID.randomUUID().toString();
        String clienteUuid = jdbc.queryForObject("SELECT clienteUuid FROM Clientes WHERE clienteId=?", String.class, parametro.getClienteId());
        jdbc.update("INSERT INTO UsuariosClientes (usuarioClienteUuid, usuarioId, usuarioUuid, clienteId, clienteUuid, nombres, apellidos, correo, prefijoTelefono, telefono, propietario, usuarioCreacionUuid) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", uuid, usuarioId, usuarioUuid, parametro.getClienteId(), clienteUuid, 
                parametro.getNombres().trim(), parametro.getApellidos(), parametro.getCorreo().trim(), parametro.getPrefijoTelefono(),
                parametro.getTelefono(), propietario, actor);
        UsuarioCliente creado = findByUuid(uuid);
        reemplazarRoles(creado.getUsuarioClienteId(), parametro.getRolesIds(), actor);
        return findByUuid(uuid);
    }

    @Override
    public UsuarioCliente update(UsuarioClienteActualizarParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE UsuariosClientes SET nombres=?, apellidos=?, correo=?, prefijoTelefono=?, telefono=?, "
                        + "estado=COALESCE(NULLIF(?,''),estado), fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                        + "usuarioModificacionUuid=? WHERE usuarioClienteUuid=? AND estado<>'Eliminado'",
                parametro.getNombres().trim(), parametro.getApellidos(), parametro.getCorreo().trim(), parametro.getPrefijoTelefono(),
                parametro.getTelefono(), parametro.getEstado(), actor, parametro.getUsuarioClienteUuid());
        if (actualizados == 0) return null;
        UsuarioCliente actualizado = findByUuid(parametro.getUsuarioClienteUuid());
        jdbc.update("UPDATE Usuarios SET estado=?, fechaModificacion=NOW(3), usuarioModificacionUuid=? WHERE usuarioId=?",
                actualizado.getEstado(), actor, actualizado.getUsuarioId());
        if (parametro.getRolesIds() != null) reemplazarRoles(actualizado.getUsuarioClienteId(), parametro.getRolesIds(), actor);
        return findByUuid(parametro.getUsuarioClienteUuid());
    }

    @Override
    public UsuarioCliente actualizarPerfil(Long usuarioClienteId, UsuarioClientePerfilParameters parametro, String actor) {
        int actualizados = jdbc.update("UPDATE UsuariosClientes SET nombres=?, apellidos=?, documento=?, correo=?, prefijoTelefono=?, telefono=?, "
                        + "whatsapp=?, direccion=?, fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                        + "usuarioModificacionUuid=? WHERE usuarioClienteId=? AND estado<>'Eliminado'",
                parametro.getNombres().trim(), parametro.getApellidos(), parametro.getDocumento(), parametro.getCorreo().trim(),
                parametro.getPrefijoTelefono(), parametro.getTelefono(), parametro.getWhatsapp(), parametro.getDireccion(), actor, usuarioClienteId);
        return actualizados == 0 ? null : findById(usuarioClienteId);
    }

    @Override
    public UsuarioCliente findById(Long id) {
        return completar(jdbc.query(SELECT + "WHERE x.usuarioClienteId=? AND x.estado<>'Eliminado'", new BeanPropertyRowMapper<>(UsuarioCliente.class), id)
                .stream().findFirst().orElse(null));
    }

    @Override
    public UsuarioCliente findByUuid(String uuid) {
        return completar(jdbc.query(SELECT + "WHERE x.usuarioClienteUuid=?", new BeanPropertyRowMapper<>(UsuarioCliente.class), uuid)
                .stream().findFirst().orElse(null));
    }

    @Override
    public List<UsuarioCliente> findAll(Long clienteId) {
        List<UsuarioCliente> usuarios = jdbc.query(SELECT + "WHERE x.clienteId=? AND x.estado<>'Eliminado' ORDER BY x.propietario DESC, x.nombres, x.apellidos",
                new BeanPropertyRowMapper<>(UsuarioCliente.class), clienteId);
        usuarios.forEach(this::completar);
        return usuarios;
    }

    @Override
    public boolean existsUsuario(String usuario) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM Usuarios WHERE usuario=? AND estado<>'Eliminado'", Integer.class, usuario.trim());
        return total != null && total > 0;
    }

    @Override
    public boolean rolesValidos(Long clienteId, List<Long> rolesIds) {
        if (rolesIds == null || rolesIds.isEmpty()) return true;
        for (Long rolId : rolesIds) {
            Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM RolesClientes WHERE rolClienteId=? AND clienteId=? AND estado='Activo'",
                    Integer.class, rolId, clienteId);
            if (total == null || total == 0) return false;
        }
        return true;
    }

    @Override
    public void reemplazarRoles(Long usuarioClienteId, List<Long> rolesIds, String actor) {
        jdbc.update("DELETE FROM UsuariosClientesRoles WHERE usuarioClienteId=?", usuarioClienteId);
        if (rolesIds == null) return;
        for (Long rolId : rolesIds.stream().distinct().toList()) {
            jdbc.update("INSERT IGNORE INTO UsuariosClientesRoles (usuarioClienteRolUuid, usuarioClienteId, usuarioClienteUuid, rolClienteId, rolClienteUuid, usuarioCreacionUuid) "
                    + "SELECT UUID(), x.usuarioClienteId, x.usuarioClienteUuid, r.rolClienteId, r.rolClienteUuid, ? FROM UsuariosClientes x CROSS JOIN RolesClientes r "
                    + "WHERE x.usuarioClienteId=? AND r.rolClienteId=?", actor, usuarioClienteId, rolId);
        }
    }

    @Override
    public void actualizarContrasena(Long usuarioClienteId, String contrasenaHash, String actor) {
        jdbc.update("UPDATE Usuarios u JOIN UsuariosClientes x ON x.usuarioId=u.usuarioId SET u.contrasena=?, u.debeCambiarContrasena=TRUE, "
                + "u.fechaModificacion=NOW(3), u.fechaVersion=NOW(3), u.version=u.version+1, u.usuarioModificacionUuid=? WHERE x.usuarioClienteId=?",
                contrasenaHash, actor, usuarioClienteId);
    }

    @Override
    public boolean delete(Long id, String actor) {
        UsuarioCliente existente = findById(id);
        if (existente == null) return false;
        jdbc.update("UPDATE Usuarios SET estado='Eliminado', fechaModificacion=NOW(3), usuarioModificacionUuid=? WHERE usuarioId=?",
                actor, existente.getUsuarioId());
        return jdbc.update("UPDATE UsuariosClientes SET estado='Eliminado', fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, "
                + "usuarioModificacionUuid=? WHERE usuarioClienteId=? AND estado<>'Eliminado'", actor, id) > 0;
    }

    private UsuarioCliente completar(UsuarioCliente usuario) {
        if (usuario != null) {
            usuario.setRolesIds(jdbc.queryForList("SELECT rolClienteId FROM UsuariosClientesRoles WHERE usuarioClienteId=? AND estado='Activo'",
                    Long.class, usuario.getUsuarioClienteId()));
            usuario.setRolesNombres(jdbc.queryForList("SELECT r.nombre FROM UsuariosClientesRoles x JOIN RolesClientes r ON r.rolClienteId=x.rolClienteId WHERE x.usuarioClienteId=? AND x.estado='Activo'",
                    String.class, usuario.getUsuarioClienteId()));
        }
        return usuario;
    }
}
