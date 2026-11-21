package com.aarondevs.luminis.Services.Suscripciones;

import com.aarondevs.luminis.DAO.Administracion.PlanDataSet;
import com.aarondevs.luminis.DAO.Suscripciones.SuscripcionSolicitudDataSet;
import com.aarondevs.luminis.Models.Administracion.Plan;
import com.aarondevs.luminis.Models.Suscripciones.SuscripcionSolicitud;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionAsignarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudAgregarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudResponderParameters;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuscripcionSolicitudServiceImpl implements SuscripcionSolicitudService {
    private final SuscripcionSolicitudDataSet datos;
    private final PlanDataSet planes;
    private final SuscripcionService suscripciones;

    public SuscripcionSolicitudServiceImpl(SuscripcionSolicitudDataSet datos, PlanDataSet planes, SuscripcionService suscripciones) {
        this.datos = datos;
        this.planes = planes;
        this.suscripciones = suscripciones;
    }

    @Override
    @Transactional
    public SuscripcionSolicitud crear(SuscripcionSolicitudAgregarParameters parametro, Long clienteId, String actor) {
        if (parametro == null || parametro.getPlanId() == null) throw new IllegalArgumentException("Seleccione el plan que desea solicitar");
        String tipoCobro = parametro.getTipoCobro() == null ? SuscripcionService.PREPAGO : parametro.getTipoCobro().trim().toUpperCase();
        if (!Set.of(SuscripcionService.PREPAGO, SuscripcionService.POSTPAGO).contains(tipoCobro)) {
            throw new IllegalArgumentException("tipoCobro debe ser PREPAGO o POSTPAGO");
        }
        if (parametro.getComentario() != null && parametro.getComentario().length() > 500) {
            throw new IllegalArgumentException("El comentario admite hasta 500 caracteres");
        }
        Plan plan = planes.findById(parametro.getPlanId());
        if (plan == null || !"Activo".equals(plan.getEstado()) || !Boolean.TRUE.equals(plan.getPublico())) {
            throw new IllegalArgumentException("El plan seleccionado no está disponible");
        }
        if (datos.existsPendiente(clienteId)) throw new IllegalArgumentException("Ya tiene una solicitud pendiente de respuesta");
        String comentario = parametro.getComentario() == null || parametro.getComentario().isBlank() ? null : parametro.getComentario().trim();
        return datos.findById(datos.create(clienteId, plan.getPlanId(), tipoCobro, comentario, actor));
    }

    @Override
    @Transactional
    public SuscripcionSolicitud responder(SuscripcionSolicitudResponderParameters parametro, String actor, Long usuarioAdministracionId) {
        if (parametro == null || parametro.getSuscripcionSolicitudId() == null || parametro.getAprobar() == null) {
            throw new IllegalArgumentException("suscripcionSolicitudId y aprobar son requeridos");
        }
        SuscripcionSolicitud solicitud = datos.findById(parametro.getSuscripcionSolicitudId());
        if (solicitud == null) throw new IllegalArgumentException("La solicitud no existe");
        if (!"Pendiente".equals(solicitud.getEstado())) throw new IllegalArgumentException("La solicitud ya fue respondida");
        String respuesta = parametro.getRespuesta() == null || parametro.getRespuesta().isBlank() ? null : parametro.getRespuesta().trim();
        if (respuesta != null && respuesta.length() > 500) throw new IllegalArgumentException("La respuesta admite hasta 500 caracteres");
        if (Boolean.TRUE.equals(parametro.getAprobar())) {
            SuscripcionAsignarParameters asignar = new SuscripcionAsignarParameters();
            asignar.setClienteId(solicitud.getClienteId());
            asignar.setPlanId(solicitud.getPlanId());
            asignar.setTipoCobro(solicitud.getTipoCobro());
            asignar.setPermitirAdicionales(parametro.getPermitirAdicionales());
            suscripciones.asignar(asignar, actor, usuarioAdministracionId);
        }
        datos.responder(solicitud.getSuscripcionSolicitudId(), Boolean.TRUE.equals(parametro.getAprobar()) ? "Aprobada" : "Rechazada",
                respuesta, usuarioAdministracionId, actor);
        return datos.findById(solicitud.getSuscripcionSolicitudId());
    }

    @Override
    public boolean cancelar(Long suscripcionSolicitudId, String actor) {
        return datos.cancelar(suscripcionSolicitudId, actor) > 0;
    }

    @Override
    public SuscripcionSolicitud id(Long suscripcionSolicitudId) {
        return datos.findById(suscripcionSolicitudId);
    }

    @Override
    public Object pages(Long clienteId, String estado, Integer page, Integer size) {
        return datos.findAllPages(clienteId, estado, page, size);
    }
}
