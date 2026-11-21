package com.aarondevs.luminis.Controllers.Mensajeria;

import com.aarondevs.luminis.Models.Mensajeria.ApiKey;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyActualizarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyAgregarParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyIdParameters;
import com.aarondevs.luminis.Parameters.Mensajeria.ApiKeyPorClienteParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Mensajeria.ApiKeyService;
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
@RequestMapping("/api/mensajeria/apikeys")
public class ApiKeyController {
    private final ApiKeyService service;
    private final ContextoAutenticacionService contextoService;

    public ApiKeyController(ApiKeyService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody ApiKeyAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "apikeys", "crear");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.clienteId(), contexto.usuarioUuid()),
                    "API key creada. Cópiela ahora: no se volverá a mostrar"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody ApiKeyActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "apikeys", "crear");
            ApiKey actual = service.id(parametro == null ? null : parametro.getApiKeyId());
            if (actual == null) return noEncontrado();
            contextoService.validarCliente(contexto, actual.getClienteId());
            return ResponseEntity.ok(ApiResponse.success(200, service.actualizar(parametro, contexto.usuarioUuid()),
                    "API key actualizada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestBody(required = false) ApiKeyPorClienteParameters parametro,
                                                 HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "listar", "apikeys", "listar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro == null ? null : parametro.getClienteId());
            if (clienteId == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.ok(ApiResponse.success(200, service.all(clienteId), "API keys obtenidas correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @GetMapping("/getPermisos")
    public ResponseEntity<ApiResponse<?>> getPermisos(HttpServletRequest request) {
        try {
            contextoService.exigirAccion(contextoService.resolver(request), "clientes", "listar", "apikeys", "listar");
            return ResponseEntity.ok(ApiResponse.success(200, ApiKeyService.PERMISOS, "Permisos obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody ApiKeyIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccionCliente(contexto, "apikeys", "revocar");
            ApiKey actual = service.id(parametro == null ? null : parametro.getApiKeyId());
            if (actual == null) return noEncontrado();
            contextoService.validarCliente(contexto, actual.getClienteId());
            service.revocar(actual.getApiKeyId(), contexto.usuarioUuid());
            return ResponseEntity.ok(ApiResponse.success(200, true, "API key revocada correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    private ResponseEntity<ApiResponse<?>> noEncontrado() {
        return ResponseEntity.status(404).body(ApiResponse.error(404, "API key no encontrada"));
    }

    private ResponseEntity<ApiResponse<?>> error(Exception error) {
        return error instanceof SecurityException
                ? ResponseEntity.status(403).body(ApiResponse.error(403, error.getMessage()))
                : ResponseEntity.badRequest().body(ApiResponse.error(400, error.getMessage()));
    }
}
