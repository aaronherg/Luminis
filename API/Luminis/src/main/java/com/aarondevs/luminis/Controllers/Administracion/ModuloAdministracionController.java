package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.RolAdministracionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administracion/modulos")
public class ModuloAdministracionController {
    private final RolAdministracionService service;
    private final ContextoAutenticacionService contextoService;

    public ModuloAdministracionController(RolAdministracionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "roles", "listar");
            return ResponseEntity.ok(ApiResponse.success(200, service.modulos(), "Módulos obtenidos correctamente"));
        } catch (Exception error) {
            return error instanceof SecurityException
                    ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                    : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
        }
    }
}
