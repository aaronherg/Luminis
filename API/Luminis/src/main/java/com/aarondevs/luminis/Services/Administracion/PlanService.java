package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.PlanActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanAgregarParameters;
import java.time.LocalDateTime;
import java.util.Map;

public interface PlanService {
    Map<String, Integer> MODALIDADES_MESES = Map.of(
            "Mensual", 1, "Bimestral", 2, "Trimestral", 3, "Semestral", 6, "Anual", 12);

    static LocalDateTime calcularFechaFin(LocalDateTime inicio, String modalidad) {
        return inicio.plusMonths(MODALIDADES_MESES.getOrDefault(modalidad, 1));
    }

    Object crear(PlanAgregarParameters parametro, String actor);

    Object actualizar(PlanActualizarParameters parametro, String actor);

    Object id(Long id);

    Object all();

    Object publicos();

    boolean eliminar(Long id, String actor);
}
