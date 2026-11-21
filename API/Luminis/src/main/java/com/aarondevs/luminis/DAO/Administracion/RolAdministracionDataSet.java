package com.aarondevs.luminis.DAO.Administracion;

import com.aarondevs.luminis.Models.Administracion.ModuloAdministracion;
import com.aarondevs.luminis.Models.Administracion.RolAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionAgregarParameters;
import java.util.List;

public interface RolAdministracionDataSet {
    RolAdministracion create(RolAdministracionAgregarParameters parametro, String actor);

    RolAdministracion update(RolAdministracionActualizarParameters parametro, String actor);

    RolAdministracion findById(Long id);

    RolAdministracion findByUuid(String uuid);

    List<RolAdministracion> findAll();

    boolean existsNombre(String nombre, String excluirUuid);

    boolean enUso(Long id);

    void reemplazarAcciones(Long rolAdministracionId, List<Long> accionesIds, String actor);

    boolean delete(Long id, String actor);

    List<ModuloAdministracion> modulos();
}
