package com.aarondevs.luminis.Services.Clientes;

import com.aarondevs.luminis.DAO.Clientes.RolClienteDataSet;
import com.aarondevs.luminis.Models.Clientes.RolCliente;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteAgregarParameters;
import org.springframework.stereotype.Service;

@Service
public class RolClienteServiceImpl implements RolClienteService {
    private final RolClienteDataSet datos;

    public RolClienteServiceImpl(RolClienteDataSet datos) {
        this.datos = datos;
    }

    @Override
    public Object crear(RolClienteAgregarParameters parametro, String actor) {
        validar(parametro, null);
        return datos.create(parametro, actor);
    }

    @Override
    public Object actualizar(RolClienteActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getRolClienteUuid() == null || parametro.getRolClienteUuid().isBlank()) {
            throw new IllegalArgumentException("rolClienteUuid es requerido");
        }
        validar(parametro, parametro.getRolClienteUuid());
        return datos.update(parametro, actor);
    }

    @Override
    public RolCliente id(Long id) {
        return datos.findById(id);
    }

    @Override
    public RolCliente uuid(String uuid) {
        return datos.findByUuid(uuid);
    }

    @Override
    public Object all(Long clienteId) {
        return datos.findAll(clienteId);
    }

    @Override
    public Object modulos() {
        return datos.modulos();
    }

    @Override
    public boolean eliminar(Long id, String actor) {
        RolCliente rol = datos.findById(id);
        if (rol == null) return false;
        if (Boolean.TRUE.equals(rol.getProtegido())) throw new IllegalArgumentException("El rol protegido no se puede eliminar");
        if (datos.enUso(id)) throw new IllegalArgumentException("El rol está asignado a uno o más usuarios");
        return datos.delete(id, actor);
    }

    private void validar(RolClienteAgregarParameters parametro, String uuid) {
        if (parametro == null || parametro.getNombre() == null || parametro.getNombre().isBlank()) {
            throw new IllegalArgumentException("nombre es requerido");
        }
        if (datos.existsNombre(parametro.getClienteId(), parametro.getNombre(), uuid)) {
            throw new IllegalArgumentException("Ya existe un rol con ese nombre");
        }
    }
}
