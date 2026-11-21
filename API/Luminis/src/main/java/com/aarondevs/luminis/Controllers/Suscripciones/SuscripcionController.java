package com.aarondevs.luminis.Controllers.Suscripciones;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionAsignarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionBuscarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.SuscripcionIdParameters;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import com.aarondevs.luminis.Services.Suscripciones.SuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {
    private final SuscripcionService service;
    private final ContextoAutenticacionService contextoService;

    public SuscripcionController(SuscripcionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody SuscripcionAsignarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "suscripciones", "asignar");
            return ResponseEntity.status(201).body(ApiResponse.success(201,
                    service.asignar(parametro, contexto.usuarioUuid(), contexto.usuarioAdministracionId()), "Plan asignado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAllPages")
    public ResponseEntity<ApiResponse<?>> getAllPages(@RequestBody(required = false) SuscripcionBuscarParameters parametro,
                                                      HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "suscripciones", "listar");
            SuscripcionBuscarParameters p = parametro == null ? new SuscripcionBuscarParameters() : parametro;
            return ResponseEntity.ok(ApiResponse.success(200, service.pages(p.getPage(), p.getSize(), p.getBuscar()),
                    "Suscripciones obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody(required = false) SuscripcionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "suscripciones", "listar", "suscripcion", "consultar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro == null ? null : parametro.getClienteId());
            if (clienteId == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.ok(ApiResponse.success(200, service.consumo(clienteId), "Suscripción obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/cancelar")
    public ResponseEntity<ApiResponse<?>> cancelar(@RequestBody SuscripcionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "suscripciones", "cancelar");
            int total = service.cancelar(parametro == null ? null : parametro.getClienteId(), contexto.usuarioUuid());
            return total == 0
                    ? ResponseEntity.status(404).body(ApiResponse.error(404, "El cliente no tiene un plan vigente"))
                    : ResponseEntity.ok(ApiResponse.success(200, true, "Plan cancelado correctamente"));
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
