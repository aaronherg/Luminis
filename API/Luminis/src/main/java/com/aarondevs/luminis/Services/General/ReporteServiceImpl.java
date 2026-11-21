package com.aarondevs.luminis.Services.General;

import com.aarondevs.luminis.DAO.General.ReporteDataSet;
import com.aarondevs.luminis.Parameters.General.ReporteParameters;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReporteServiceImpl implements ReporteService {
    private static final int MAXIMO_DIAS = 366;
    private static final List<String> CAMPOS = List.of("peticiones", "mensajes", "entregados", "vistos", "desaparecidos", "rechazados",
            "solicitudesApi", "conexiones", "bytes");

    private final ReporteDataSet datos;
    private final SuscripcionService suscripciones;

    public ReporteServiceImpl(ReporteDataSet datos, SuscripcionService suscripciones) {
        this.datos = datos;
        this.suscripciones = suscripciones;
    }

    @Override
    public Map<String, Object> ingresos(ReporteParameters parametro) {
        LocalDate[] rango = rango(parametro);
        List<Map<String, Object>> porDia = datos.ingresosPorDia(rango[0], rango[1]);
        Map<String, Object> resultado = encabezado(rango);
        resultado.put("total", sumar(porDia, "monto"));
        resultado.put("pagos", porDia.stream().mapToLong(f -> numero(f.get("pagos")).longValue()).sum());
        resultado.put("porDia", porDia);
        resultado.put("porMetodo", datos.ingresosPorMetodo(rango[0], rango[1]));
        resultado.put("detalle", datos.ingresosDetalle(rango[0], rango[1]));
        return resultado;
    }

    @Override
    public Map<String, Object> cuentasPorCobrar() {
        List<Map<String, Object>> cargos = datos.cuentasPorCobrar();
        Map<String, BigDecimal> tramos = new LinkedHashMap<>();
        for (String tramo : List.of("Por vencer", "1 a 30 días", "31 a 60 días", "61 a 90 días", "Más de 90 días")) tramos.put(tramo, BigDecimal.ZERO);
        for (Map<String, Object> cargo : cargos) {
            long dias = numero(cargo.get("diasVencido")).longValue();
            String tramo = dias <= 0 ? "Por vencer" : dias <= 30 ? "1 a 30 días" : dias <= 60 ? "31 a 60 días" : dias <= 90 ? "61 a 90 días" : "Más de 90 días";
            cargo.put("tramo", tramo);
            tramos.merge(tramo, decimal(cargo.get("montoPendiente")), BigDecimal::add);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total", sumar(cargos, "montoPendiente"));
        resultado.put("cargos", cargos.size());
        resultado.put("vencido", cargos.stream().filter(c -> numero(c.get("diasVencido")).longValue() > 0)
                .map(c -> decimal(c.get("montoPendiente"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        resultado.put("tramos", tramos.entrySet().stream().map(e -> Map.of("tramo", (Object) e.getKey(), "monto", e.getValue())).toList());
        resultado.put("detalle", cargos);
        return resultado;
    }

    @Override
    public Map<String, Object> consumoClientes(ReporteParameters parametro) {
        List<Map<String, Object>> clientes = datos.consumoClientes(parametro == null ? null : parametro.getBuscar());
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("peticiones", clientes.stream().mapToLong(f -> numero(f.get("peticionesPeriodo")).longValue()).sum());
        resultado.put("mensajes30", clientes.stream().mapToLong(f -> numero(f.get("mensajes30")).longValue()).sum());
        resultado.put("clientes", clientes.size());
        resultado.put("detalle", clientes);
        return resultado;
    }

    @Override
    public Map<String, Object> trafico(ReporteParameters parametro) {
        LocalDate[] rango = rango(parametro);
        List<Map<String, Object>> porDia = datos.traficoPorDia(null, rango[0], rango[1]);
        Map<String, Object> resultado = encabezado(rango);
        resultado.put("totales", totales(porDia));
        resultado.put("porDia", porDia);
        resultado.put("detalle", datos.traficoPorCliente(rango[0], rango[1]));
        return resultado;
    }

    @Override
    public Map<String, Object> consumoCliente(Long clienteId, ReporteParameters parametro) {
        LocalDate[] rango = rango(parametro);
        List<Map<String, Object>> porDia = datos.traficoPorDia(clienteId, rango[0], rango[1]);
        Map<String, Object> resultado = encabezado(rango);
        resultado.put("totales", totales(porDia));
        resultado.put("porDia", porDia);
        resultado.put("plan", suscripciones.consumo(clienteId));
        resultado.put("apiKeys", datos.usoApiKeys(clienteId));
        return resultado;
    }

    @Override
    public Map<String, Object> entregasCliente(Long clienteId, ReporteParameters parametro) {
        LocalDate[] rango = rango(parametro);
        List<Map<String, Object>> porDia = datos.traficoPorDia(clienteId, rango[0], rango[1]);
        Map<String, Object> totales = totales(porDia);
        long mensajes = numero(totales.get("mensajes")).longValue();
        Map<String, Object> resultado = encabezado(rango);
        resultado.put("totales", totales);
        resultado.put("tasaEntrega", mensajes == 0 ? 0 : numero(totales.get("entregados")).doubleValue() * 100 / mensajes);
        resultado.put("tasaVisto", mensajes == 0 ? 0 : numero(totales.get("vistos")).doubleValue() * 100 / mensajes);
        resultado.put("porDia", porDia);
        return resultado;
    }

    private LocalDate[] rango(ReporteParameters parametro) {
        LocalDate hasta = parametro == null || parametro.getHasta() == null ? LocalDate.now() : parametro.getHasta();
        LocalDate desde = parametro == null || parametro.getDesde() == null ? hasta.minusDays(29) : parametro.getDesde();
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha desde no puede ser mayor que la fecha hasta");
        if (ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS) throw new IllegalArgumentException("El rango máximo es de un año");
        return new LocalDate[] {desde, hasta};
    }

    private Map<String, Object> encabezado(LocalDate[] rango) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", rango[0]);
        resultado.put("hasta", rango[1]);
        return resultado;
    }

    private Map<String, Object> totales(List<Map<String, Object>> filas) {
        Map<String, Object> totales = new LinkedHashMap<>();
        for (String campo : CAMPOS) totales.put(campo, filas.stream().mapToLong(f -> numero(f.get(campo)).longValue()).sum());
        totales.put("picoConexiones", filas.stream().mapToLong(f -> numero(f.get("picoConexiones")).longValue()).max().orElse(0));
        return totales;
    }

    private BigDecimal sumar(List<Map<String, Object>> filas, String campo) {
        return filas.stream().map(f -> decimal(f.get(campo))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal decimal(Object valor) {
        if (valor instanceof BigDecimal d) return d;
        return valor instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }

    private Number numero(Object valor) {
        return valor instanceof Number n ? n : 0;
    }
}
