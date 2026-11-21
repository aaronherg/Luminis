package com.aarondevs.luminis.DAO.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.SuscripcionSolicitud;
import com.aarondevs.luminis.Parameters.Response.PaginationResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SuscripcionSolicitudDAO implements SuscripcionSolicitudDataSet {
    private static final String FROM = "FROM SuscripcionesSolicitudes x JOIN Clientes c ON c.clienteId=x.clienteId JOIN Planes p ON p.planId=x.planId ";
    private static final String SELECT = "SELECT x.*, c.nombreComercial AS clienteNombre, p.nombre AS planNombre " + FROM;

    private final JdbcTemplate jdbc;

    public SuscripcionSolicitudDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PaginationResponse<SuscripcionSolicitud> findAllPages(Long clienteId, String estado, Integer page, Integer size) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = size == null || size < 1 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder("WHERE x.estado<>'Eliminado' ");
        List<Object> argumentos = new ArrayList<>();
        if (clienteId != null) {
            where.append("AND x.clienteId=? ");
            argumentos.add(clienteId);
        }
        if (estado != null && !estado.isBlank()) {
            where.append("AND x.estado=? ");
            argumentos.add(estado.trim());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM + where, Long.class, argumentos.toArray());
        List<Object> argumentosPagina = new ArrayList<>(argumentos);
        argumentosPagina.add(tamano);
        argumentosPagina.add((pagina - 1) * tamano);
        List<SuscripcionSolicitud> registros = jdbc.query(SELECT + where + "ORDER BY x.fechaCreacion DESC LIMIT ? OFFSET ?",
                new BeanPropertyRowMapper<>(SuscripcionSolicitud.class), argumentosPagina.toArray());
        long totalRegistros = total == null ? 0 : total;
        int totalPaginas = (int) Math.ceil((double) totalRegistros / tamano);
        return new PaginationResponse<>(pagina, tamano, totalRegistros, totalPaginas, pagina > 1, pagina < totalPaginas, registros);
    }

    @Override
    public SuscripcionSolicitud findById(Long suscripcionSolicitudId) {
        return jdbc.query(SELECT + "WHERE x.suscripcionSolicitudId=?", new BeanPropertyRowMapper<>(SuscripcionSolicitud.class),
                suscripcionSolicitudId).stream().findFirst().orElse(null);
    }

    @Override
    public boolean existsPendiente(Long clienteId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM SuscripcionesSolicitudes WHERE clienteId=? AND estado='Pendiente'",
                Integer.class, clienteId);
        return total != null && total > 0;
    }

    @Override
    public Long create(Long clienteId, Long planId, String tipoCobro, String comentario, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO SuscripcionesSolicitudes (suscripcionSolicitudUuid, clienteId, clienteUuid, planId, planUuid, tipoCobro, "
                        + "comentario, estado, usuarioCreacionUuid) SELECT ?, c.clienteId, c.clienteUuid, p.planId, p.planUuid, ?, ?, 'Pendiente', ? "
                        + "FROM Clientes c CROSS JOIN Planes p WHERE c.clienteId=? AND p.planId=?",
                uuid, tipoCobro, comentario, actor, clienteId, planId);
        return jdbc.queryForObject("SELECT suscripcionSolicitudId FROM SuscripcionesSolicitudes WHERE suscripcionSolicitudUuid=?", Long.class, uuid);
    }

    @Override
    public int responder(Long suscripcionSolicitudId, String estado, String respuesta, Long usuarioAdministracionId, String actor) {
        return jdbc.update("UPDATE SuscripcionesSolicitudes SET estado=?, respuesta=?, fechaRespuesta=NOW(3), usuarioAdministracionId=?, "
                        + "usuarioAdministracionUuid=(SELECT u.usuarioAdministracionUuid FROM UsuariosAdministracion u WHERE u.usuarioAdministracionId=?), "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? "
                        + "WHERE suscripcionSolicitudId=? AND estado='Pendiente'",
                estado, respuesta, usuarioAdministracionId, usuarioAdministracionId, actor, suscripcionSolicitudId);
    }

    @Override
    public int cancelar(Long suscripcionSolicitudId, String actor) {
        return jdbc.update("UPDATE SuscripcionesSolicitudes SET estado='Cancelada', fechaModificacion=NOW(3), fechaVersion=NOW(3), "
                + "version=version+1, usuarioModificacionUuid=? WHERE suscripcionSolicitudId=? AND estado='Pendiente'", actor, suscripcionSolicitudId);
    }
}
