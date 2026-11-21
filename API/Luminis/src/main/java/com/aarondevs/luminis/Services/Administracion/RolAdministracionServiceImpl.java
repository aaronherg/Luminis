package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.DAO.Administracion.RolAdministracionDataSet;
import com.aarondevs.luminis.Models.Administracion.RolAdministracion;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionAgregarParameters;
import org.springframework.stereotype.Service;

@Service
public class RolAdministracionServiceImpl implements RolAdministracionService {
    private final RolAdministracionDataSet datos;

    public RolAdministracionServiceImpl(RolAdministracionDataSet datos) {
        this.datos = datos;
    }

    @Override
    public Object crear(RolAdministracionAgregarParameters parametro, String actor) {
        validar(parametro, null);
        return datos.create(parametro, actor);
    }

    @Override
    public Object actualizar(RolAdministracionActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getRolAdministracionUuid() == null || parametro.getRolAdministracionUuid().isBlank()) {
            throw new IllegalArgumentException("rolAdministracionUuid es requerido");
        }
        validar(parametro, parametro.getRolAdministracionUuid());
        return datos.update(parametro, actor);
    }

    @Override
    public RolAdministracion id(Long id) {
        return datos.findById(id);
    }

    @Override
    public RolAdministracion uuid(String uuid) {
        return datos.findByUuid(uuid);
    }

    @Override
    public Object all() {
        return datos.findAll();
    }

    @Override
    public Object modulos() {
        return datos.modulos();
    }

    @Override
    public boolean eliminar(Long id, String actor) {
        RolAdministracion rol = datos.findById(id);
        if (rol == null) return false;
        if (Boolean.TRUE.equals(rol.getProtegido())) throw new IllegalArgumentException("El rol protegido no se puede eliminar");
        if (datos.enUso(id)) throw new IllegalArgumentException("El rol está asignado a uno o más usuarios");
        return datos.delete(id, actor);
    }

    private void validar(RolAdministracionAgregarParameters parametro, String uuid) {
        if (parametro == null || parametro.getNombre() == null || parametro.getNombre().isBlank()) {
            throw new IllegalArgumentException("nombre es requerido");
        }
        if (datos.existsNombre(parametro.getNombre(), uuid)) {
            throw new IllegalArgumentException("Ya existe un rol con ese nombre");
        }
    }
}
