package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionAgregarParameters;
import com.aarondevs.luminis.Parameters.Administracion.RolAdministracionIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.RolAdministracionService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administracion/roles")
public class RolAdministracionController {
    private final RolAdministracionService service;
    private final ContextoAutenticacionService contextoService;

    public RolAdministracionController(RolAdministracionService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody RolAdministracionAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "roles", "crear");
            if (parametro == null) throw new IllegalArgumentException("Los datos del rol son requeridos");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.usuarioUuid()),
                    "Rol creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody RolAdministracionActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "roles", "modificar");
            if (parametro == null) throw new IllegalArgumentException("Los datos del rol son requeridos");
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Rol actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAdministracion(contexto);
            return ResponseEntity.ok(ApiResponse.success(200, service.all(), "Roles obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody RolAdministracionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "roles", "listar");
            var resultado = parametro == null ? null : service.id(parametro.getRolAdministracionId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Rol obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody RolAdministracionIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "roles", "eliminar");
            var rol = parametro == null ? null : service.id(parametro.getRolAdministracionId());
            if (rol == null) return noEncontrado();
            boolean eliminado = service.eliminar(parametro.getRolAdministracionId(), contexto.usuarioUuid());
            return eliminado ? ResponseEntity.ok(ApiResponse.success(200, null, "Rol eliminado correctamente")) : noEncontrado();
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Rol no encontrado"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
