package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.MonedaActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaAgregarParameters;

public interface MonedaService {
    Object crear(MonedaAgregarParameters parametro, String actor);

    Object actualizar(MonedaActualizarParameters parametro, String actor);

    Object id(Long id);

    Object all();

    boolean eliminar(Long id, String actor);
}
