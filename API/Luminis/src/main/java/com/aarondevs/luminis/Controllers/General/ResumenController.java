package com.aarondevs.luminis.Controllers.General;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.General.ResumenService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/general/resumen")
public class ResumenController {
    private final ResumenService service;
    private final ContextoAutenticacionService contextoService;

    public ResumenController(ResumenService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @GetMapping("/getAdministracion")
    public ResponseEntity<ApiResponse<?>> getAdministracion(HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "inicio", "consultar");
            return ResponseEntity.ok(ApiResponse.success(200, service.administracion(), "Resumen obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getCliente")
    public ResponseEntity<ApiResponse<?>> getCliente(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "inicio", "consultar");
            return ResponseEntity.ok(ApiResponse.success(200, service.cliente(contexto.clienteId()), "Resumen obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getRed")
    public ResponseEntity<ApiResponse<?>> getRed() {
        try {
            return ResponseEntity.ok(ApiResponse.success(200, service.red(), "Estado de la red obtenido correctamente"));
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
