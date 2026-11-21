package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.DAO.Administracion.MonedaDataSet;
import com.aarondevs.luminis.Parameters.Administracion.MonedaActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaAgregarParameters;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MonedaServiceImpl implements MonedaService {
    private final MonedaDataSet datos;

    public MonedaServiceImpl(MonedaDataSet datos) {
        this.datos = datos;
    }

    @Override
    public Object crear(MonedaAgregarParameters parametro, String actor) {
        validar(parametro, null);
        return datos.create(parametro, actor);
    }

    @Override
    public Object actualizar(MonedaActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getMonedaUuid() == null || parametro.getMonedaUuid().isBlank()) {
            throw new IllegalArgumentException("monedaUuid es requerido");
        }
        validar(parametro, parametro.getMonedaUuid());
        if (parametro.getEstado() != null && !Set.of("Activo", "Inactivo").contains(parametro.getEstado())) {
            throw new IllegalArgumentException("estado debe ser Activo o Inactivo");
        }
        return datos.update(parametro, actor);
    }

    @Override
    public Object id(Long id) {
        return datos.findById(id);
    }

    @Override
    public Object all() {
        return datos.findAll();
    }

    @Override
    public boolean eliminar(Long id, String actor) {
        if (id != null && datos.enUso(id)) throw new IllegalArgumentException("La moneda está asignada a uno o más planes");
        return datos.delete(id, actor);
    }

    private void validar(MonedaAgregarParameters parametro, String uuid) {
        if (parametro == null || vacio(parametro.getCodigo()) || vacio(parametro.getNombre()) || vacio(parametro.getSimbolo())
                || parametro.getTasa() == null) {
            throw new IllegalArgumentException("codigo, nombre, simbolo y tasa son requeridos");
        }
        if (parametro.getTasa().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("La tasa debe ser mayor que cero");
        if (datos.existsCodigo(parametro.getCodigo(), uuid)) throw new IllegalArgumentException("Ya existe una moneda con ese código");
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
