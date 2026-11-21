package com.aarondevs.luminis.Services.General;

import com.aarondevs.luminis.Parameters.General.ReporteParameters;
import java.util.Map;

public interface ReporteService {
    Map<String, Object> ingresos(ReporteParameters parametro);

    Map<String, Object> cuentasPorCobrar();

    Map<String, Object> consumoClientes(ReporteParameters parametro);

    Map<String, Object> trafico(ReporteParameters parametro);

    Map<String, Object> consumoCliente(Long clienteId, ReporteParameters parametro);

    Map<String, Object> entregasCliente(Long clienteId, ReporteParameters parametro);
}
