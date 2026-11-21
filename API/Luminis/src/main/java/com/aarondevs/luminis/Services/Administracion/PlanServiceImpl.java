package com.aarondevs.luminis.Services.Administracion;

import com.aarondevs.luminis.DAO.Administracion.PlanDataSet;
import com.aarondevs.luminis.Parameters.Administracion.PlanActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanAgregarParameters;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PlanServiceImpl implements PlanService {
    private final PlanDataSet datos;

    public PlanServiceImpl(PlanDataSet datos) {
        this.datos = datos;
    }

    @Override
    public Object crear(PlanAgregarParameters parametro, String actor) {
        validar(parametro);
        return datos.create(parametro, actor);
    }

    @Override
    public Object actualizar(PlanActualizarParameters parametro, String actor) {
        if (parametro == null || parametro.getPlanUuid() == null || parametro.getPlanUuid().isBlank()) {
            throw new IllegalArgumentException("planUuid es requerido");
        }
        validar(parametro);
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
    public Object publicos() {
        return datos.findPublicos();
    }

    @Override
    public boolean eliminar(Long id, String actor) {
        if (id != null && datos.enUso(id)) throw new IllegalArgumentException("El plan tiene suscripciones vigentes");
        return datos.delete(id, actor);
    }

    private void validar(PlanAgregarParameters parametro) {
        if (parametro == null || parametro.getMonedaId() == null || parametro.getNombre() == null || parametro.getNombre().isBlank()
                || parametro.getModalidad() == null
                || parametro.getPrecio() == null) {
            throw new IllegalArgumentException("monedaId, nombre, modalidad y precio son requeridos");
        }
        if (!MODALIDADES_MESES.containsKey(parametro.getModalidad())) {
            throw new IllegalArgumentException("modalidad debe ser Mensual, Bimestral, Trimestral, Semestral o Anual");
        }
        if (parametro.getPeticionesMes() != null && parametro.getPeticionesMes() <= 0) throw new IllegalArgumentException("Las peticiones al mes deben ser mayores que cero");
        if (parametro.getConexionesSimultaneas() != null && parametro.getConexionesSimultaneas() <= 0) {
            throw new IllegalArgumentException("Las conexiones simultáneas deben ser mayores que cero");
        }
        if (parametro.getTamanoMaximoMensajeKb() != null && (parametro.getTamanoMaximoMensajeKb() <= 0 || parametro.getTamanoMaximoMensajeKb() > 1024)) {
            throw new IllegalArgumentException("El tamaño máximo del mensaje debe estar entre 1 y 1024 KB");
        }
        if (parametro.getRetencionSegundos() == null) parametro.setRetencionSegundos(10);
        if (parametro.getRetencionSegundos() < 0 || parametro.getRetencionSegundos() > 3600) {
            throw new IllegalArgumentException("La retención de la cola offline debe estar entre 0 y 3600 segundos");
        }
        if (parametro.getPrecio().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio no puede ser negativo");
        if (parametro.getPrecioMilAdicional() == null || parametro.getPeticionesMes() == null) parametro.setPrecioMilAdicional(BigDecimal.ZERO);
        if (parametro.getPrecioMilAdicional().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio por cada 1,000 peticiones adicionales no puede ser negativo");
        }
        if (!datos.existsMoneda(parametro.getMonedaId())) throw new IllegalArgumentException("monedaId no es válido");
    }
}
