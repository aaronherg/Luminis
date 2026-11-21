package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.Plan;
import com.aarondevs.luminis.Parameters.Administracion.PlanActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanAgregarParameters;
import java.util.List;

public interface PlanDataSet {
    Plan create(PlanAgregarParameters parametro, String actor);

    Plan update(PlanActualizarParameters parametro, String actor);

    Plan findById(Long id);

    Plan findByUuid(String uuid);

    List<Plan> findAll();

    List<Plan> findPublicos();

    boolean existsMoneda(Long monedaId);

    boolean enUso(Long id);

    boolean delete(Long id, String actor);
}
