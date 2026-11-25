package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.ApiKeyAdministracionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administracion/apikeys")
public class ApiKeyAdministracionController {
    private final ApiKeyAdministracionService service;
    private final ContextoAutenticacionService contextoService;

    public ApiKeyAdministracionController(ApiKeyAdministracionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            var c = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(c, "apikeysadmin", "consultar");
            return ResponseEntity.ok(ApiResponse.success(200, service.listar(c), "API keys obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody(required = false) Map<String, Object> p, HttpServletRequest request) {
        try {
            var c = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(c, "apikeysadmin", "crear");
            String nombre = p == null || p.get("nombre") == null ? null : p.get("nombre").toString();
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(c, nombre), "API key creada. Cópiela ahora: no se volverá a mostrar"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/rename")
    public ResponseEntity<ApiResponse<?>> rename(@RequestBody(required = false) Map<String, Object> p, HttpServletRequest request) {
        try {
            var c = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(c, "apikeysadmin", "editar");
            String uuid = p == null || p.get("uuid") == null ? null : p.get("uuid").toString();
            String nombre = p == null || p.get("nombre") == null ? null : p.get("nombre").toString();
            return ResponseEntity.ok(ApiResponse.success(200, service.renombrar(c, uuid, nombre), "API key actualizada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/revoke")
    public ResponseEntity<ApiResponse<?>> revoke(@RequestBody(required = false) Map<String, Object> p, HttpServletRequest request) {
        try {
            var c = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(c, "apikeysadmin", "revocar");
            String uuid = p == null || p.get("uuid") == null ? null : p.get("uuid").toString();
            service.revocar(c, uuid);
            return ResponseEntity.ok(ApiResponse.success(200, null, "API key revocada correctamente"));
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
