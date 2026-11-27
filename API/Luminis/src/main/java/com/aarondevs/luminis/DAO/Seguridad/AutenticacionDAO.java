package com.aarondevs.luminis.DAO.Seguridad;

import com.aarondevs.luminis.Models.Seguridad.Usuario;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AutenticacionDAO implements AutenticacionDataSet {
    private final JdbcTemplate jdbc;

    public AutenticacionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Usuario findUsuarioByUsuario(String usuario) {
        return jdbc.query("SELECT * FROM Usuarios WHERE usuario=? AND estado<>'Eliminado'",
                new BeanPropertyRowMapper<>(Usuario.class), usuario).stream().findFirst().orElse(null);
    }

    @Override
    public Usuario findUsuarioById(Long usuarioId) {
        return jdbc.query("SELECT * FROM Usuarios WHERE usuarioId=? AND estado<>'Eliminado'",
                new BeanPropertyRowMapper<>(Usuario.class), usuarioId).stream().findFirst().orElse(null);
    }

    @Override
    public Map<String, Object> perfilAdministracion(Long usuarioId) {
        return jdbc.queryForList("SELECT ua.usuarioAdministracionId, ua.usuarioAdministracionUuid, ua.nombres, ua.apellidos, ua.correo, ua.estado "
                + "FROM UsuariosAdministracion ua WHERE ua.usuarioId=?", usuarioId).stream().findFirst().orElse(null);
    }

    @Override
    public Map<String, Object> perfilCliente(Long usuarioId) {
        return jdbc.queryForList("SELECT uc.usuarioClienteId, uc.usuarioClienteUuid, uc.nombres, uc.apellidos, uc.correo, uc.propietario, uc.estado, "
                + "c.clienteId, c.clienteUuid, c.nombreComercial AS clienteNombre, c.estado AS clienteEstado "
                + "FROM UsuariosClientes uc JOIN Clientes c ON c.clienteId=uc.clienteId WHERE uc.usuarioId=?", usuarioId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public void crear(Long usuarioId, String usuarioUuid, String tipo, String jti, String ip, String agente, Timestamp expiracion) {
        jdbc.update("INSERT INTO Autenticaciones (autenticacionUuid, usuarioId, usuarioUuid, tipoUsuario, tokenJti, ipPublica, agente, fechaExpiracion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID().toString(), usuarioId, usuarioUuid, tipo, jti, ip,
                agente == null ? null : agente.substring(0, Math.min(agente.length(), 300)), expiracion);
    }

    @Override
    public void registrarAcceso(Long usuarioId) {
        jdbc.update("UPDATE Usuarios SET fechaUltimoAcceso=NOW(3) WHERE usuarioId=?", usuarioId);
    }

    @Override
    public boolean cerrar(String jti, String motivo) {
        return jdbc.update("UPDATE Autenticaciones SET estado='Cerrada', fechaCierre=NOW(3), motivoCierre=? WHERE tokenJti=? AND estado='Activa'",
                motivo, jti) > 0;
    }

    @Override
    public void actualizarContrasena(Long usuarioId, String contrasenaHash, String actor) {
        jdbc.update("UPDATE Usuarios SET contrasena=?, debeCambiarContrasena=FALSE, fechaModificacion=NOW(3), fechaVersion=NOW(3), "
                + "version=version+1, usuarioModificacionUuid=? WHERE usuarioId=?", contrasenaHash, actor, usuarioId);
    }

    @Override
    public void actualizarDosFactores(Long usuarioId, boolean activo) {
        jdbc.update("UPDATE Usuarios SET dosFactores=?, fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1 WHERE usuarioId=?", activo, usuarioId);
    }

    @Override
    public Map<String, Object> cuentaAdministracion(Long usuarioAdministracionId) {
        Map<String, Object> r = new LinkedHashMap<>(jdbc.queryForList("SELECT ua.nombres, ua.apellidos, ua.correo, ua.estado, ua.fechaCreacion, u.usuario, u.fechaUltimoAcceso "
                + "FROM UsuariosAdministracion ua JOIN Usuarios u ON u.usuarioId=ua.usuarioId WHERE ua.usuarioAdministracionId=?", usuarioAdministracionId).stream().findFirst().orElse(Map.of()));
        r.put("roles", jdbc.queryForList("SELECT r.nombre FROM UsuariosAdministracionRoles ur JOIN RolesAdministracion r ON r.rolAdministracionId=ur.rolAdministracionId AND r.estado<>'Eliminado' "
                + "WHERE ur.usuarioAdministracionId=? AND ur.estado='Activo' ORDER BY r.nombre", String.class, usuarioAdministracionId));
        return r;
    }

    @Override
    public Map<String, Object> cuentaCuenta(Long clienteId, Long usuarioClienteId) {
        Map<String, Object> r = new LinkedHashMap<>(jdbc.queryForList("SELECT c.nombreComercial, c.razonSocial, c.tipoCliente, c.documento, c.correo, c.prefijoTelefono, c.telefono, c.direccion, c.fechaCreacion, c.estado, "
                + "(SELECT COUNT(*) FROM UsuariosClientes x WHERE x.clienteId=c.clienteId AND x.estado<>'Eliminado') AS usuarios FROM Clientes c WHERE c.clienteId=?", clienteId).stream().findFirst().orElse(Map.of()));
        r.put("usuario", jdbc.queryForList("SELECT uc.nombres, uc.apellidos, uc.correo, uc.propietario, u.usuario, u.fechaUltimoAcceso FROM UsuariosClientes uc JOIN Usuarios u ON u.usuarioId=uc.usuarioId "
                + "WHERE uc.usuarioClienteId=?", usuarioClienteId).stream().findFirst().orElse(Map.of()));
        r.put("roles", jdbc.queryForList("SELECT r.nombre FROM UsuariosClientesRoles ur JOIN RolesClientes r ON r.rolClienteId=ur.rolClienteId AND r.estado<>'Eliminado' "
                + "WHERE ur.usuarioClienteId=? AND ur.estado='Activo' ORDER BY r.nombre", String.class, usuarioClienteId));
        return r;
    }
}
