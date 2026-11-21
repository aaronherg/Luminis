package com.aarondevs.luminis.DAO.General;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResumenDAO implements ResumenDataSet {
    private static final String VIGENTE = "d.estado='Activo' AND d.fechaInicio<=NOW(3) AND d.fechaFin>NOW(3)";

    private final JdbcTemplate jdbc;

    public ResumenDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, Object> totalesAdministracion() {
        return jdbc.queryForMap("SELECT "
                + "(SELECT COUNT(*) FROM Clientes WHERE estado='Activo') AS clientesActivos, "
                + "(SELECT COUNT(*) FROM Clientes WHERE estado='Suspendido') AS clientesSuspendidos, "
                + "(SELECT COUNT(DISTINCT s.clienteId) FROM DetallesSuscripciones d JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId WHERE "
                + VIGENTE + ") AS suscripcionesVigentes, "
                + "(SELECT COUNT(*) FROM SuscripcionesSolicitudes WHERE estado='Pendiente') AS solicitudesPendientes, "
                + "(SELECT COUNT(*) FROM PagosSuscripciones WHERE estado='Pendiente') AS cargosPendientes, "
                + "(SELECT COALESCE(SUM(montoTotal-montoPagado),0) FROM PagosSuscripciones WHERE estado='Pendiente') AS montoPendiente, "
                + "(SELECT COALESCE(SUM(monto),0) FROM PagosSuscripcionesDetalles WHERE estado='Activo' "
                + "AND fechaPago>=DATE_FORMAT(NOW(),'%Y-%m-01')) AS ingresosMes, "
                + "(SELECT COALESCE(SUM(peticiones),0) FROM MetricasClientesDiarias WHERE fecha=CURRENT_DATE) AS peticionesHoy, "
                + "(SELECT COALESCE(SUM(mensajes),0) FROM MetricasClientesDiarias WHERE fecha=CURRENT_DATE) AS mensajesHoy, "
                + "(SELECT COALESCE(SUM(entregados),0) FROM MetricasClientesDiarias WHERE fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS entregados30, "
                + "(SELECT COALESCE(SUM(mensajes),0) FROM MetricasClientesDiarias WHERE fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS mensajes30");
    }

    @Override
    public Map<String, Object> totalesRed() {
        return jdbc.queryForMap("SELECT "
                + "(SELECT COUNT(*) FROM Clientes WHERE estado='Activo') AS proyectos, "
                + "(SELECT COALESCE(SUM(mensajes),0) FROM MetricasClientesDiarias WHERE fecha=CURRENT_DATE) AS eventosHoy, "
                + "(SELECT COALESCE(SUM(entregados),0) FROM MetricasClientesDiarias WHERE fecha=CURRENT_DATE) AS entregadosHoy, "
                + "(SELECT COALESCE(SUM(entregados),0) FROM MetricasClientesDiarias WHERE fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS entregados30, "
                + "(SELECT COALESCE(SUM(mensajes),0) FROM MetricasClientesDiarias WHERE fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS eventos30");
    }

    @Override
    public List<Map<String, Object>> distribucionPlanes() {
        return jdbc.queryForList("SELECT p.planId, p.nombre, COUNT(d.detalleSuscripcionId) AS clientes FROM Planes p "
                + "LEFT JOIN DetallesSuscripciones d ON d.planId=p.planId AND " + VIGENTE + " "
                + "WHERE p.estado<>'Eliminado' GROUP BY p.planId, p.nombre, p.orden ORDER BY p.orden, p.nombre");
    }

    @Override
    public List<Map<String, Object>> ultimosPagos(int limite) {
        return jdbc.queryForList("SELECT pd.monto, pd.metodo, pd.fechaPago, c.nombreComercial AS clienteNombre, m.simbolo AS monedaSimbolo "
                + "FROM PagosSuscripcionesDetalles pd JOIN PagosSuscripciones g ON g.pagoSuscripcionId=pd.pagoSuscripcionId "
                + "JOIN DetallesSuscripciones d ON d.detalleSuscripcionId=g.detalleSuscripcionId "
                + "JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId "
                + "JOIN Clientes c ON c.clienteId=s.clienteId JOIN Monedas m ON m.monedaId=g.monedaId "
                + "WHERE pd.estado='Activo' ORDER BY pd.fechaPago DESC LIMIT ?", limite);
    }

    @Override
    public List<Map<String, Object>> ingresosMensuales(int meses) {
        return jdbc.queryForList("SELECT DATE_FORMAT(fechaPago,'%Y-%m') AS mes, SUM(monto) AS monto FROM PagosSuscripcionesDetalles "
                + "WHERE estado='Activo' AND fechaPago>=DATE_SUB(DATE_FORMAT(NOW(),'%Y-%m-01'), INTERVAL ? MONTH) "
                + "GROUP BY DATE_FORMAT(fechaPago,'%Y-%m') ORDER BY mes", meses - 1);
    }

    @Override
    public List<Map<String, Object>> traficoGlobal(int dias) {
        return jdbc.queryForList("SELECT fecha, SUM(peticiones) AS peticiones, SUM(mensajes) AS mensajes, SUM(entregados) AS entregados "
                + "FROM MetricasClientesDiarias WHERE fecha>=DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) GROUP BY fecha ORDER BY fecha", dias - 1);
    }

    @Override
    public Map<String, Object> totalesCliente(Long clienteId) {
        return jdbc.queryForMap("SELECT "
                + "(SELECT COUNT(*) FROM ApiKeys WHERE clienteId=? AND estado='Activo') AS apiKeysActivas, "
                + "(SELECT COUNT(*) FROM UsuariosClientes WHERE clienteId=? AND estado='Activo') AS usuarios, "
                + "(SELECT COALESCE(SUM(g.montoTotal-g.montoPagado),0) FROM PagosSuscripciones g "
                + "JOIN DetallesSuscripciones d ON d.detalleSuscripcionId=g.detalleSuscripcionId "
                + "JOIN Suscripciones s ON s.suscripcionId=d.suscripcionId WHERE s.clienteId=? AND g.estado='Pendiente') AS montoPendiente, "
                + "(SELECT COALESCE(SUM(mensajes),0) FROM MetricasClientesDiarias WHERE clienteId=? AND fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS mensajes30, "
                + "(SELECT COALESCE(SUM(entregados),0) FROM MetricasClientesDiarias WHERE clienteId=? AND fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS entregados30, "
                + "(SELECT COALESCE(MAX(picoConexiones),0) FROM MetricasClientesDiarias WHERE clienteId=? AND fecha>=DATE_SUB(CURRENT_DATE, INTERVAL 29 DAY)) AS picoConexiones30",
                clienteId, clienteId, clienteId, clienteId, clienteId, clienteId);
    }

    @Override
    public List<Map<String, Object>> metricasCliente(Long clienteId, int dias) {
        return jdbc.queryForList("SELECT fecha, peticiones, mensajes, entregados, vistos, desaparecidos, rechazados, solicitudesApi, conexiones, "
                + "picoConexiones, bytes FROM MetricasClientesDiarias WHERE clienteId=? AND fecha>=DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) ORDER BY fecha",
                clienteId, dias - 1);
    }
}
