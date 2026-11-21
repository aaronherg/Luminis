package com.aarondevs.luminis.DAO.Seguridad;

import com.aarondevs.luminis.Security.ContextoAutenticacion;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContextoAutenticacionDAO implements ContextoAutenticacionDataSet {
    private final JdbcTemplate jdbc;

    public ContextoAutenticacionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ContextoAutenticacion resolver(String usuarioUuid, String tipo, String tokenJti) {
        if (usuarioUuid == null || tipo == null || tokenJti == null) return null;
        Integer sesion = jdbc.queryForObject(
                "SELECT COUNT(*) FROM Autenticaciones a JOIN Usuarios u ON u.usuarioId=a.usuarioId "
                        + "WHERE a.tokenJti=? AND a.estado='Activa' AND a.fechaExpiracion>NOW(3) "
                        + "AND u.usuarioUuid=? AND u.estado='Activo'",
                Integer.class, tokenJti, usuarioUuid);
        if (sesion == null || sesion == 0) return null;
        if (ContextoAutenticacion.ADMINISTRACION.equalsIgnoreCase(tipo)) {
            return jdbc.query(
                    "SELECT u.usuarioId, u.usuarioUuid, ua.usuarioAdministracionId, ua.usuarioAdministracionUuid "
                            + "FROM Usuarios u JOIN UsuariosAdministracion ua ON ua.usuarioId=u.usuarioId "
                            + "WHERE u.usuarioUuid=? AND u.tipo=? AND ua.estado='Activo'",
                    (rs, i) -> new ContextoAutenticacion(rs.getLong("usuarioId"), rs.getString("usuarioUuid"),
                            ContextoAutenticacion.ADMINISTRACION, tokenJti, null, null, null, null,
                            rs.getLong("usuarioAdministracionId"), rs.getString("usuarioAdministracionUuid")),
                    usuarioUuid, ContextoAutenticacion.ADMINISTRACION).stream().findFirst().orElse(null);
        }
        if (ContextoAutenticacion.CLIENTE.equalsIgnoreCase(tipo)) {
            return jdbc.query(
                    "SELECT u.usuarioId, u.usuarioUuid, uc.usuarioClienteId, uc.usuarioClienteUuid, c.clienteId, c.clienteUuid "
                            + "FROM Usuarios u JOIN UsuariosClientes uc ON uc.usuarioId=u.usuarioId "
                            + "JOIN Clientes c ON c.clienteId=uc.clienteId "
                            + "WHERE u.usuarioUuid=? AND u.tipo=? AND uc.estado='Activo' AND c.estado='Activo'",
                    (rs, i) -> new ContextoAutenticacion(rs.getLong("usuarioId"), rs.getString("usuarioUuid"),
                            ContextoAutenticacion.CLIENTE, tokenJti, rs.getLong("clienteId"), rs.getString("clienteUuid"),
                            rs.getLong("usuarioClienteId"), rs.getString("usuarioClienteUuid"), null, null),
                    usuarioUuid, ContextoAutenticacion.CLIENTE).stream().findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public Set<String> accionesAdministracion(Long usuarioAdministracionId) {
        return new LinkedHashSet<>(jdbc.queryForList(
                "SELECT DISTINCT CONCAT(m.clave,'.',a.clave) FROM UsuariosAdministracionRoles ur "
                        + "JOIN RolesAdministracion r ON r.rolAdministracionId=ur.rolAdministracionId AND r.estado='Activo' "
                        + "JOIN RolesAdministracionAcciones ra ON ra.rolAdministracionId=r.rolAdministracionId AND ra.estado='Activo' "
                        + "JOIN ModulosAdministracionAcciones a ON a.moduloAdministracionAccionId=ra.moduloAdministracionAccionId AND a.estado='Activo' "
                        + "JOIN ModulosAdministracion m ON m.moduloAdministracionId=a.moduloAdministracionId AND m.estado='Activo' "
                        + "WHERE ur.usuarioAdministracionId=? AND ur.estado='Activo'",
                String.class, usuarioAdministracionId));
    }

    @Override
    public Set<String> accionesCliente(Long usuarioClienteId) {
        return new LinkedHashSet<>(jdbc.queryForList(
                "SELECT DISTINCT CONCAT(m.clave,'.',a.clave) FROM UsuariosClientesRoles ur "
                        + "JOIN RolesClientes r ON r.rolClienteId=ur.rolClienteId AND r.estado='Activo' "
                        + "JOIN RolesClientesAcciones ra ON ra.rolClienteId=r.rolClienteId AND ra.estado='Activo' "
                        + "JOIN ModulosClientesAcciones a ON a.moduloClienteAccionId=ra.moduloClienteAccionId AND a.estado='Activo' "
                        + "JOIN ModulosClientes m ON m.moduloClienteId=a.moduloClienteId AND m.estado='Activo' "
                        + "WHERE ur.usuarioClienteId=? AND ur.estado='Activo'",
                String.class, usuarioClienteId));
    }

    @Override
    public void registrarActividad(String tokenJti) {
        jdbc.update("UPDATE Autenticaciones SET fechaUltimaActividad=NOW(3) WHERE tokenJti=? "
                + "AND (fechaUltimaActividad IS NULL OR fechaUltimaActividad<DATE_SUB(NOW(3), INTERVAL 1 MINUTE))", tokenJti);
    }
}
