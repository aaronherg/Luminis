package com.aarondevs.luminis.DAO.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.DetalleSuscripcion;
import com.aarondevs.luminis.Models.Suscripciones.Suscripcion;
import com.aarondevs.luminis.Parameters.Response.PaginationResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SuscripcionDAO implements SuscripcionDataSet {
    private static final String SELECT_DETALLE = "SELECT d.*, p.nombre AS planNombre, p.peticionesMes, p.conexionesSimultaneas, p.tamanoMaximoMensajeKb, "
            + "p.retencionSegundos, p.precio, p.precioMilAdicional, m.simbolo AS monedaSimbolo FROM DetallesSuscripciones d "
            + "JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId JOIN Planes p ON p.planId=d.planId "
            + "JOIN Monedas m ON m.monedaId=p.monedaId ";
    private static final String SELECT_SUSCRIPCION = "SELECT s.*, c.nombreComercial AS clienteNombre FROM Suscripciones s "
            + "JOIN Clientes c ON c.clienteId=s.clienteId ";

    private final JdbcTemplate jdbc;

    public SuscripcionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Suscripcion findByClienteId(Long clienteId) {
        Suscripcion suscripcion = jdbc.query(SELECT_SUSCRIPCION + "WHERE s.clienteId=?", new BeanPropertyRowMapper<>(Suscripcion.class), clienteId)
                .stream().findFirst().orElse(null);
        if (suscripcion != null) suscripcion.setVigente(findVigente(clienteId));
        return suscripcion;
    }

    @Override
    public PaginationResponse<Suscripcion> findAllPages(Integer page, Integer size, String buscar) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = size == null || size < 1 ? 20 : Math.min(size, 200);
        String where = "WHERE c.estado<>'Eliminado' ";
        List<Object> argumentos = new ArrayList<>();
        if (buscar != null && !buscar.isBlank()) {
            where += "AND (c.nombreComercial LIKE ? OR c.documento LIKE ?) ";
            argumentos.add("%" + buscar.trim() + "%");
            argumentos.add("%" + buscar.trim() + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM Suscripciones s JOIN Clientes c ON c.clienteId=s.clienteId " + where,
                Long.class, argumentos.toArray());
        List<Object> argumentosPagina = new ArrayList<>(argumentos);
        argumentosPagina.add(tamano);
        argumentosPagina.add((pagina - 1) * tamano);
        List<Suscripcion> registros = jdbc.query(SELECT_SUSCRIPCION + where + "ORDER BY c.nombreComercial LIMIT ? OFFSET ?",
                new BeanPropertyRowMapper<>(Suscripcion.class), argumentosPagina.toArray());
        registros.forEach(s -> s.setVigente(findVigente(s.getClienteId())));
        int totalPaginas = (int) Math.ceil((double) total / tamano);
        return new PaginationResponse<>(pagina, tamano, total, totalPaginas, pagina > 1, pagina < totalPaginas, registros);
    }

    @Override
    public DetalleSuscripcion findVigente(Long clienteId) {
        return jdbc.query(SELECT_DETALLE + "WHERE s.clienteId=? AND d.estado='Activo' AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3) "
                + "ORDER BY d.fechaInicio DESC LIMIT 1", new BeanPropertyRowMapper<>(DetalleSuscripcion.class), clienteId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public DetalleSuscripcion findDetalleById(Long detalleSuscripcionId) {
        return jdbc.query(SELECT_DETALLE + "WHERE d.detalleSuscripcionId=?", new BeanPropertyRowMapper<>(DetalleSuscripcion.class),
                detalleSuscripcionId).stream().findFirst().orElse(null);
    }

    @Override
    public List<DetalleSuscripcion> findDetalles(Long clienteId) {
        return jdbc.query(SELECT_DETALLE + "WHERE s.clienteId=? AND d.estado<>'Eliminado' ORDER BY d.fechaInicio DESC LIMIT 60",
                new BeanPropertyRowMapper<>(DetalleSuscripcion.class), clienteId);
    }

    @Override
    public int cerrarVigente(Long clienteId, String estado, String actor) {
        return jdbc.update("UPDATE DetallesSuscripciones d JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId "
                + "SET d.fechaFin=NOW(3), d.estado=?, d.renovacionAutomatica=FALSE, d.fechaModificacion=NOW(3), d.fechaVersion=NOW(3), "
                + "d.version=d.version+1, d.usuarioModificacionUuid=? "
                + "WHERE s.clienteId=? AND d.estado='Activo' AND d.fechaFin>NOW(3)", estado, actor, clienteId);
    }

    @Override
    public Long createDetalle(Long suscripcionId, Long planId, LocalDateTime inicio, LocalDateTime fin, String tipoCobro,
                              boolean permitirAdicionales, boolean renovacionAutomatica, Long renovacionOrigenDetalleId, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO DetallesSuscripciones (detalleSuscripcionUuid, suscripcionId, suscripcionUuid, planId, planUuid, fechaInicio, "
                        + "fechaFin, tipoCobro, permitirAdicionales, renovacionAutomatica, renovacionOrigenDetalleId, renovacionOrigenDetalleUuid, "
                        + "usuarioCreacionUuid) SELECT ?, s.suscripcionId, s.suscripcionUuid, p.planId, p.planUuid, ?, ?, ?, ?, ?, ?, "
                        + "(SELECT o.detalleSuscripcionUuid FROM DetallesSuscripciones o WHERE o.detalleSuscripcionId=?), ? "
                        + "FROM Suscripciones s CROSS JOIN Planes p WHERE s.suscripcionId=? AND p.planId=?",
                uuid, Timestamp.valueOf(inicio), Timestamp.valueOf(fin), tipoCobro, permitirAdicionales, renovacionAutomatica,
                renovacionOrigenDetalleId, renovacionOrigenDetalleId, actor, suscripcionId, planId);
        return jdbc.queryForObject("SELECT detalleSuscripcionId FROM DetallesSuscripciones WHERE detalleSuscripcionUuid=?", Long.class, uuid);
    }

    @Override
    public Long createPago(Long detalleSuscripcionId, BigDecimal montoPlan, LocalDateTime fechaVencimiento, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO PagosSuscripciones (pagoSuscripcionUuid, detalleSuscripcionId, detalleSuscripcionUuid, monedaId, monedaUuid, "
                        + "montoPlan, montoTotal, fechaVencimiento, estado, usuarioCreacionUuid) "
                        + "SELECT ?, d.detalleSuscripcionId, d.detalleSuscripcionUuid, p.monedaId, p.monedaUuid, ?, ?, ?, 'Pendiente', ? "
                        + "FROM DetallesSuscripciones d JOIN Planes p ON p.planId=d.planId WHERE d.detalleSuscripcionId=?",
                uuid, montoPlan, montoPlan, Timestamp.valueOf(fechaVencimiento), actor, detalleSuscripcionId);
        return jdbc.queryForObject("SELECT pagoSuscripcionId FROM PagosSuscripciones WHERE pagoSuscripcionUuid=?", Long.class, uuid);
    }

    @Override
    public List<DetalleSuscripcion> findPendientesRenovacion(LocalDateTime limite) {
        return jdbc.query(SELECT_DETALLE + "JOIN Clientes c ON c.clienteId=s.clienteId "
                        + "WHERE d.estado='Activo' AND d.renovacionAutomatica=TRUE AND UPPER(d.tipoCobro)='POSTPAGO' AND d.fechaFin<=? "
                        + "AND c.estado='Activo' AND p.estado='Activo' AND NOT EXISTS (SELECT 1 FROM DetallesSuscripciones n "
                        + "WHERE n.renovacionOrigenDetalleId=d.detalleSuscripcionId) ORDER BY d.fechaFin",
                new BeanPropertyRowMapper<>(DetalleSuscripcion.class), Timestamp.valueOf(limite));
    }

    @Override
    public boolean existsRenovacion(Long detalleSuscripcionId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM DetallesSuscripciones WHERE renovacionOrigenDetalleId=?",
                Integer.class, detalleSuscripcionId);
        return total != null && total > 0;
    }

    @Override
    public long peticionesPeriodo(Long clienteId, LocalDateTime desde, LocalDateTime hasta) {
        Long total = jdbc.queryForObject("SELECT COALESCE(SUM(peticiones),0) FROM MetricasClientesDiarias WHERE clienteId=? AND fecha>=? AND fecha<=?",
                Long.class, clienteId, Date.valueOf(desde.toLocalDate()), Date.valueOf(hasta.toLocalDate()));
        return total == null ? 0 : total;
    }

    @Override
    public void registrarAdicionales(Long detalleSuscripcionId, long peticionesAdicionales, BigDecimal montoAdicionales) {
        jdbc.update("UPDATE PagosSuscripciones SET peticionesAdicionales=?, montoAdicionales=?, montoTotal=montoPlan+?, "
                + "estado=CASE WHEN montoPagado>=montoPlan+? THEN 'Pagado' ELSE 'Pendiente' END, fechaModificacion=NOW(3), "
                + "fechaVersion=NOW(3), version=version+1 WHERE detalleSuscripcionId=?",
                peticionesAdicionales, montoAdicionales, montoAdicionales, montoAdicionales, detalleSuscripcionId);
    }
}
