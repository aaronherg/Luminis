package com.aarondevs.luminis.Controllers.Suscripciones;

import com.aarondevs.luminis.Models.Suscripciones.PagoSuscripcion;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Parameters.Suscripciones.PagoSuscripcionAgregarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.PagoSuscripcionBuscarParameters;
import com.aarondevs.luminis.Parameters.Suscripciones.PagoSuscripcionIdParameters;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import com.aarondevs.luminis.Services.Suscripciones.PagoSuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripciones/pagos")
public class PagoSuscripcionController {
    private final PagoSuscripcionService service;
    private final ContextoAutenticacionService contextoService;

    public PagoSuscripcionController(PagoSuscripcionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody PagoSuscripcionAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "pagos", "registrar");
            if (parametro == null) throw new IllegalArgumentException("Los datos del pago son requeridos");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.registrar(parametro.getPagoSuscripcionId(),
                    parametro.getMonto(), parametro.getMetodo(), parametro.getReferencia(), parametro.getObservacion(),
                    contexto.usuarioUuid(), contexto.usuarioAdministracionId()), "Pago registrado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAllPages")
    public ResponseEntity<ApiResponse<?>> getAllPages(@RequestBody(required = false) PagoSuscripcionBuscarParameters parametro,
                                                      HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "pagos", "listar", "pagos", "listar");
            PagoSuscripcionBuscarParameters p = parametro == null ? new PagoSuscripcionBuscarParameters() : parametro;
            Long clienteId = contextoService.clienteDeContexto(contexto, p.getClienteId());
            return ResponseEntity.ok(ApiResponse.success(200, service.pages(clienteId, p.getEstado(), p.getBuscar(), p.getPage(), p.getSize()),
                    "Pagos obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody PagoSuscripcionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "pagos", "listar", "pagos", "listar");
            PagoSuscripcion pago = parametro == null ? null : service.id(parametro.getPagoSuscripcionId());
            if (pago == null) return ResponseEntity.status(404).body(ApiResponse.error(404, "Cargo no encontrado"));
            contextoService.validarCliente(contexto, pago.getClienteId());
            return ResponseEntity.ok(ApiResponse.success(200, pago, "Pago obtenido correctamente"));
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
