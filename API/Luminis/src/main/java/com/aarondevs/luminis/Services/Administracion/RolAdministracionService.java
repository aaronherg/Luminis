package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.Models.Administracion.RolAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionAgregarParameters;

public interface RolAdministracionService {
    Object crear(RolAdministracionAgregarParameters parametro, String actor);

    Object actualizar(RolAdministracionActualizarParameters parametro, String actor);

    RolAdministracion id(Long id);

    RolAdministracion uuid(String uuid);

    Object all();

    Object modulos();

    boolean eliminar(Long id, String actor);
}
