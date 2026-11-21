package com.aarondevs.luminis.DAO.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.PagoSuscripcion;
import com.aarondevs.luminis.Models.Suscripciones.PagoSuscripcionDetalle;
import com.aarondevs.luminis.Parameters.Response.PaginationResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PagoSuscripcionDAO implements PagoSuscripcionDataSet {
    private static final String FROM = "FROM PagosSuscripciones g JOIN DetallesSuscripciones d ON d.detalleSuscripcionId=g.detalleSuscripcionId "
            + "JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId JOIN Clientes c ON c.clienteId=s.clienteId "
            + "JOIN Planes p ON p.planId=d.planId JOIN Monedas m ON m.monedaId=g.monedaId ";
    private static final String SELECT = "SELECT g.*, c.clienteId, c.nombreComercial AS clienteNombre, p.nombre AS planNombre, "
            + "d.fechaInicio, d.fechaFin, d.tipoCobro, m.simbolo AS monedaSimbolo, (g.montoTotal-g.montoPagado) AS montoPendiente " + FROM;

    private final JdbcTemplate jdbc;

    public PagoSuscripcionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PaginationResponse<PagoSuscripcion> findAllPages(Long clienteId, String estado, String buscar, Integer page, Integer size) {
        int pagina = page == null || page < 1 ? 1 : page;
        int tamano = size == null || size < 1 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder("WHERE g.estado<>'Eliminado' ");
        List<Object> argumentos = new ArrayList<>();
        if (clienteId != null) {
            where.append("AND c.clienteId=? ");
            argumentos.add(clienteId);
        }
        if (estado != null && !estado.isBlank()) {
            where.append("AND g.estado=? ");
            argumentos.add(estado.trim());
        }
        if (buscar != null && !buscar.isBlank()) {
            where.append("AND (c.nombreComercial LIKE ? OR p.nombre LIKE ?) ");
            argumentos.add("%" + buscar.trim() + "%");
            argumentos.add("%" + buscar.trim() + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM + where, Long.class, argumentos.toArray());
        List<Object> argumentosPagina = new ArrayList<>(argumentos);
        argumentosPagina.add(tamano);
        argumentosPagina.add((pagina - 1) * tamano);
        List<PagoSuscripcion> registros = jdbc.query(SELECT + where + "ORDER BY g.fechaVencimiento DESC, g.pagoSuscripcionId DESC LIMIT ? OFFSET ?",
                new BeanPropertyRowMapper<>(PagoSuscripcion.class), argumentosPagina.toArray());
        long totalRegistros = total == null ? 0 : total;
        int totalPaginas = (int) Math.ceil((double) totalRegistros / tamano);
        return new PaginationResponse<>(pagina, tamano, totalRegistros, totalPaginas, pagina > 1, pagina < totalPaginas, registros);
    }

    @Override
    public PagoSuscripcion findById(Long pagoSuscripcionId) {
        return jdbc.query(SELECT + "WHERE g.pagoSuscripcionId=?", new BeanPropertyRowMapper<>(PagoSuscripcion.class), pagoSuscripcionId)
                .stream().findFirst().orElse(null);
    }

    @Override
    public List<PagoSuscripcionDetalle> findDetalles(Long pagoSuscripcionId) {
        return jdbc.query("SELECT * FROM PagosSuscripcionesDetalles WHERE pagoSuscripcionId=? AND estado='Activo' ORDER BY fechaPago DESC",
                new BeanPropertyRowMapper<>(PagoSuscripcionDetalle.class), pagoSuscripcionId);
    }

    @Override
    public Long createDetalle(Long pagoSuscripcionId, BigDecimal monto, String metodo, String referencia, String observacion,
                              Long usuarioAdministracionId, String actor) {
        String uuid = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO PagosSuscripcionesDetalles (pagoDetalleUuid, pagoSuscripcionId, pagoSuscripcionUuid, monto, metodo, referencia, "
                        + "observacion, usuarioAdministracionId, usuarioAdministracionUuid, usuarioCreacionUuid) "
                        + "SELECT ?, g.pagoSuscripcionId, g.pagoSuscripcionUuid, ?, ?, ?, ?, ?, "
                        + "(SELECT u.usuarioAdministracionUuid FROM UsuariosAdministracion u WHERE u.usuarioAdministracionId=?), ? "
                        + "FROM PagosSuscripciones g WHERE g.pagoSuscripcionId=?",
                uuid, monto, metodo, referencia, observacion, usuarioAdministracionId, usuarioAdministracionId, actor, pagoSuscripcionId);
        return jdbc.queryForObject("SELECT pagoDetalleId FROM PagosSuscripcionesDetalles WHERE pagoDetalleUuid=?", Long.class, uuid);
    }

    @Override
    public void aplicarMonto(Long pagoSuscripcionId, BigDecimal monto, String actor) {
        jdbc.update("UPDATE PagosSuscripciones SET montoPagado=montoPagado+?, "
                        + "estado=CASE WHEN montoPagado>=montoTotal THEN 'Pagado' ELSE 'Pendiente' END, "
                        + "fechaModificacion=NOW(3), fechaVersion=NOW(3), version=version+1, usuarioModificacionUuid=? WHERE pagoSuscripcionId=?",
                monto, actor, pagoSuscripcionId);
    }

    @Override
    public Long clienteDeSuscripcion(Long suscripcionId) {
        return jdbc.query("SELECT clienteId FROM Suscripciones WHERE suscripcionId=?", (rs, i) -> rs.getLong(1), suscripcionId)
                .stream().findFirst().orElse(null);
    }
}
