package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.PlanActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanAgregarParameters;
import com.aarondevs.luminis.Parameters.Administracion.PlanIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.PlanService;
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
@RequestMapping("/api/administracion/planes")
public class PlanController {
    private final PlanService service;
    private final ContextoAutenticacionService contextoService;

    public PlanController(PlanService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody PlanAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "planes", "crear");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.usuarioUuid()),
                    "Plan creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody PlanActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "planes", "modificar");
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Plan actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            contextoService.exigirAdministracion(contextoService.resolver(request));
            return ResponseEntity.ok(ApiResponse.success(200, service.all(), "Planes obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getAllPublicos")
    public ResponseEntity<ApiResponse<?>> getAllPublicos() {
        return ResponseEntity.ok(ApiResponse.success(200, service.publicos(), "Planes obtenidos correctamente"));
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody PlanIdParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "planes", "listar");
            Object resultado = parametro == null ? null : service.id(parametro.getPlanId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Plan obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody PlanIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "planes", "cambiar_estado");
            boolean eliminado = parametro != null && service.eliminar(parametro.getPlanId(), contexto.usuarioUuid());
            return eliminado ? ResponseEntity.ok(ApiResponse.success(200, null, "Plan eliminado correctamente")) : noEncontrado();
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Plan no encontrado"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
