package com.aarondevs.luminis.Controllers.Clientes;

import com.aarondevs.luminis.Parameters.Clientes.RolClienteActualizarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteAgregarParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClienteIdParameters;
import com.aarondevs.luminis.Parameters.Clientes.RolClientePorClienteParameters;
import com.aarondevs.luminis.Parameters.Response.ApiResponse;
import com.aarondevs.luminis.Services.Clientes.RolClienteService;
import com.aarondevs.luminis.Services.Seguridad.ContextoAutenticacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes/roles")
public class RolClienteController {
    private final RolClienteService service;
    private final ContextoAutenticacionService contextoService;

    public RolClienteController(RolClienteService service, ContextoAutenticacionService contextoService) {
        this.service = service;
        this.contextoService = contextoService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(@RequestBody RolClienteAgregarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "roles", "crear");
            if (parametro == null) throw new IllegalArgumentException("Los datos del rol son requeridos");
            parametro.setClienteId(contextoService.clienteDeContexto(contexto, parametro.getClienteId()));
            if (parametro.getClienteId() == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.status(201).body(ApiResponse.success(201, service.crear(parametro, contexto.usuarioUuid()),
                    "Rol creado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> update(@RequestBody RolClienteActualizarParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "roles", "modificar");
            if (parametro == null) throw new IllegalArgumentException("Los datos del rol son requeridos");
            var actual = service.uuid(parametro.getRolClienteUuid());
            if (actual == null) return noEncontrado();
            contextoService.validarCliente(contexto, actual.getClienteId());
            parametro.setClienteId(actual.getClienteId());
            Object resultado = service.actualizar(parametro, contexto.usuarioUuid());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Rol actualizado correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getAll")
    public ResponseEntity<ApiResponse<?>> getAll(@RequestBody RolClientePorClienteParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "listar", "roles", "listar");
            Long clienteId = contextoService.clienteDeContexto(contexto, parametro == null ? null : parametro.getClienteId());
            if (clienteId == null) throw new IllegalArgumentException("clienteId es requerido");
            return ResponseEntity.ok(ApiResponse.success(200, service.all(clienteId), "Roles obtenidos correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getById(@RequestBody RolClienteIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "listar", "roles", "listar");
            var resultado = parametro == null ? null : service.id(parametro.getRolClienteId());
            if (resultado != null) contextoService.validarCliente(contexto, resultado.getClienteId());
            return resultado == null ? noEncontrado()
                    : ResponseEntity.ok(ApiResponse.success(200, resultado, "Rol obtenido correctamente"));
        } catch (Exception error) {
            return error(error);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody RolClienteIdParameters parametro, HttpServletRequest request) {
        try {
            var contexto = contextoService.resolver(request);
            contextoService.exigirAccion(contexto, "clientes", "modificar", "roles", "eliminar");
            var rol = parametro == null ? null : service.id(parametro.getRolClienteId());
            if (rol == null) return noEncontrado();
            contextoService.validarCliente(contexto, rol.getClienteId());
            boolean eliminado = service.eliminar(parametro.getRolClienteId(), contexto.usuarioUuid());
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
