package com.aarondevs.luminis.DAO.General;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReporteDataSet {
    List<Map<String, Object>> ingresosDetalle(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> ingresosPorDia(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> ingresosPorMetodo(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> cuentasPorCobrar();

    List<Map<String, Object>> consumoClientes(String buscar);

    List<Map<String, Object>> traficoPorCliente(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> traficoPorDia(Long clienteId, LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> usoApiKeys(Long clienteId);
}
