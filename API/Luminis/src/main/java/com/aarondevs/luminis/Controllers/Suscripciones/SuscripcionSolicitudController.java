package com.aarondevs.luminis.Controllers.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.SuscripcionSolicitud;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudAgregarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudBuscarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudIdParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionSolicitudResponderParameters;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionSolicitudService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripciones/solicitudes")
public class SuscripcionSolicitudController {
    private final SuscripcionSolicitudService service;
    private final ContextoAutenticacionService contextoService;

    public SuscripcionSolicitudController(SuscripcionSolicitudService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody SuscripcionSolicitudAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "suscripcion", "solicitar");
            return ResponseEntity.status(201).body(ApiResponse.success(201,
                    service.crear(parametro, contexto.clienteId(), contexto.usuarioUuid()), "Solicitud enviada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody SuscripcionSolicitudResponderParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "suscripciones", "gestionar_solicitudes");
            SuscripcionSolicitud resultado = service.responder(parametro, contexto.usuarioUuid(), contexto.usuarioAdministracionId());
            return ResponseEntity.ok(ApiResponse.success(200, resultado,
                    "Aprobada".equals(resultado.getEstado()) ? "Solicitud aprobada y plan asignado" : "Solicitud rechazada"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAllPages")
    public ResponseEntity<ApiResponse<?>> getAllPages(@RequestBody(required = false) SuscripcionSolicitudBuscarParameters parametro,
                                                      HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "suscripciones", "gestionar_solicitudes", "suscripcion", "consultar");
            SuscripcionSolicitudBuscarParameters p = parametro == null ? new SuscripcionSolicitudBuscarParameters() : parametro;
            Long clienteId = contextoService.clienteDeContexto(contexto, p.getClienteId());
            return ResponseEntity.ok(ApiResponse.success(200, service.pages(clienteId, p.getEstado(), p.getPage(), p.getSize()),
                    "Solicitudes obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody SuscripcionSolicitudIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "suscripciones", "gestionar_solicitudes", "suscripcion", "consultar");
            SuscripcionSolicitud solicitud = parametro == null ? null : service.id(parametro.getSuscripcionSolicitudId());
            if (solicitud == null) return ResponseEntity.status(404).body(ApiResponse.error(404, "Solicitud no encontrada"));
            contextoService.validarCliente(contexto, solicitud.getClienteId());
            return ResponseEntity.ok(ApiResponse.success(200, solicitud, "Solicitud obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody SuscripcionSolicitudIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "suscripcion", "solicitar");
            SuscripcionSolicitud solicitud = parametro == null ? null : service.id(parametro.getSuscripcionSolicitudId());
            if (solicitud == null) return ResponseEntity.status(404).body(ApiResponse.error(404, "Solicitud no encontrada"));
            contextoService.validarCliente(contexto, solicitud.getClienteId());
            return service.cancelar(solicitud.getSuscripcionSolicitudId(), contexto.usuarioUuid())
                    ? ResponseEntity.ok(ApiResponse.success(200, true, "Solicitud cancelada correctamente"))
                    : ResponseEntity.badRequest().body(ApiResponse.error(400, "Solo se pueden cancelar solicitudes pendientes"));
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
