package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.Moneda;
import com.aarondevs.luminis.Parameters.Administracion.MonedaActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaAgregarParameters;
import java.util.List;

public interface MonedaDataSet {
    Moneda create(MonedaAgregarParameters parametro, String actor);

    Moneda update(MonedaActualizarParameters parametro, String actor);

    Moneda findById(Long id);

    Moneda findByUuid(String uuid);

    List<Moneda> findAll();

    boolean existsCodigo(String codigo, String excluirUuid);

    boolean enUso(Long id);

    boolean delete(Long id, String actor);
}
