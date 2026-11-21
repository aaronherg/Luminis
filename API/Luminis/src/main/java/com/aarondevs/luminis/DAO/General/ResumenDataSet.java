package com.aarondevs.luminis.DAO.General;

import java.util.List;
import java.util.Map;

public interface ResumenDataSet {
    Map<String, Object> totalesAdministracion();

    List<Map<String, Object>> distribucionPlanes();

    List<Map<String, Object>> ultimosPagos(int limite);

    List<Map<String, Object>> ingresosMensuales(int meses);

    List<Map<String, Object>> traficoGlobal(int dias);

    Map<String, Object> totalesRed();

    Map<String, Object> totalesCliente(Long clienteId);

    List<Map<String, Object>> metricasCliente(Long clienteId, int dias);
}
