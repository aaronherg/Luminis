package com.aarondevs.luminis.Controllers.Administracion;

import com.aarondevs.luminis.Parameters.Administracion.MonedaActualizarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaAgregarParameters;
import com.aarondevs.luminis.Parameters.Administracion.MonedaIdParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Administracion.MonedaService;
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
@RequestMapping("/api/administracion/monedas")
public class MonedaController {
    private final MonedaService service;
    private final ContextoAutenticacionService contextoService;

    public MonedaController(MonedaService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody MonedaAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "monedas", "crear");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.usuarioUuid()),
                    "Moneda creada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody MonedaActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "monedas", "modificar");
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Moneda actualizada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(HttpServletRequest request) {
        try {
            contextoService.exigirAdministracion(contextoService.resolver(request));
            return ResponseEntity.ok(ApiResponse.success(200, service.all(), "Monedas obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody MonedaIdParameters parametro, HttpServletRequest request) {
        try {
            contextoService.exigirAccionAdministracion(contextoService.resolver(request), "monedas", "listar");
            Object resultado = parametro == null ? null : service.id(parametro.getMonedaId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Moneda obtenida correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody MonedaIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionAdministracion(contexto, "monedas", "modificar");
            boolean eliminado = parametro != null && service.eliminar(parametro.getMonedaId(), contexto.usuarioUuid());
            return eliminado ? ResponseEntity.ok(ApiResponse.success(200, null, "Moneda eliminada correctamente")) : noEncontrado();
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "Moneda no encontrada"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
