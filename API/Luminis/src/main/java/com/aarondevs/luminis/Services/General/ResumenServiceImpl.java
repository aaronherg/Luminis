package com.aarondevs.luminis.Services.General;

import com.aarondevs.luminis.DAO.General.ResumenDataSet;
import com.aarondevs.luminis.Services.Mensajeria.SocketHub;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ResumenServiceImpl implements ResumenService {
    private final ResumenDataSet datos;
    private final SuscripcionService suscripciones;
    private final SocketHub hub;

    public ResumenServiceImpl(ResumenDataSet datos, SuscripcionService suscripciones, SocketHub hub) {
        this.datos = datos;
        this.suscripciones = suscripciones;
        this.hub = hub;
    }

    @Override
    public Map<String, Object> administracion() {
        Map<String, Object> resultado = new LinkedHashMap<>(datos.totalesAdministracion());
        resultado.put("enVivo", hub.resumen(null));
        resultado.put("planes", datos.distribucionPlanes());
        resultado.put("ultimosPagos", datos.ultimosPagos(8));
        resultado.put("ingresosMensuales", datos.ingresosMensuales(6));
        resultado.put("trafico", datos.traficoGlobal(30));
        return resultado;
    }

    @Override
    public Map<String, Object> cliente(Long clienteId) {
        Map<String, Object> resultado = new LinkedHashMap<>(datos.totalesCliente(clienteId));
        resultado.putAll(suscripciones.consumo(clienteId));
        resultado.put("enVivo", hub.resumen(clienteId));
        resultado.put("metricas", datos.metricasCliente(clienteId, 30));
        return resultado;
    }

    @Override
    public Map<String, Object> red() {
        Map<String, Object> resultado = new LinkedHashMap<>(datos.totalesRed());
        Map<String, Object> vivo = hub.resumen(null);
        resultado.put("conexiones", vivo.get("conexiones"));
        resultado.put("usuarios", vivo.get("usuarios"));
        resultado.put("eventosProcesados", hub.eventosProcesados());
        resultado.put("momento", System.currentTimeMillis());
        return resultado;
    }
}
